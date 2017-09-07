package org.restbucks.tdd.web.rest

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.Test
import org.restbucks.tdd.application.PlaceOrderCommandHandler
import org.restbucks.tdd.command.PlaceOrderCommand
import org.restbucks.tdd.domain.ordering.Location
import org.restbucks.tdd.domain.ordering.OrderRepository
import org.restbucks.tdd.web.AbstractWebMvcTest
import org.restbucks.tdd.web.rest.assembler.OrderResourceAssembler
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cloud.contract.wiremock.restdocs.WireMockRestDocs

import static org.hamcrest.Matchers.is
import static org.mockito.BDDMockito.given
import static org.restbucks.tdd.domain.ordering.Location.TAKE_AWAY
import static org.restbucks.tdd.domain.ordering.Order.newOrder
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.*
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*
import static org.springframework.restdocs.payload.PayloadDocumentation.*
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [
    OrderRestController,
    OrderResourceAssembler
])
class OrderRestControllerTest extends AbstractWebMvcTest {

    @MockBean
    private OrderRepository orderRepository

    @MockBean
    private PlaceOrderCommandHandler placeOrderCommandHandler

    @Test
    void "it should return an order"() {

        def order = newOrder()
        order.with(TAKE_AWAY)

        given(orderRepository.findOne(order.id)).willReturn(order)

        // @formatter:off
        this.mockMvc.perform(get("/rel/orders/${order.id.value}"))
	            .andExpect(status().isOk())
                .andExpect(jsonPath("location", is(order.location.name())))
                .andDo(document('ordering/find_order_by_id',
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                fieldWithPath("location")
                                    .description("The location of the order, should be one of ${Location.values()}"),
                                subsectionWithPath("_links").ignored()//validate by links() block
                            ),
                            links(halLinks(),
                                    linkWithRel("self")
                                            .description("link to refresh the order")
                            )
                ))
        // @formatter:on
    }

    @Test
    void "it should forward place order command to command handler"() {

        def order = newOrder()
        order.with(TAKE_AWAY)

        def command = new PlaceOrderCommand(order.location)

        given(placeOrderCommandHandler.handle(command)).willReturn(order)

        // @formatter:off
        this.mockMvc.perform(post("/rel/orders/placeOrderCommand")
                                .content("""
                                    {
                                        "location": "TAKE_AWAY"
                                    }
                                """)
                                .contentType(APPLICATION_JSON_UTF8))
	            .andExpect(status().isOk())
                .andExpect(jsonPath("location", is(order.location.name())))
                .andDo(document('ordering/place_order',
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                fieldWithPath("location")
                                    .description("The location of the order, should be one of ${Location.values()}")
                            ),
                            responseFields(
                                fieldWithPath("location")
                                    .description("The location of the order, should be one of ${Location.values()}"),
                                subsectionWithPath("_links").ignored()//validate by links() block
                            ),
                            links(halLinks(),
                                    linkWithRel("self")
                                            .description("link to refresh the order")
                            )
                ))
                .andDo(WireMockRestDocs.verify()
						.wiremock(WireMock.post("/rel/orders/placeOrderCommand"))
						.stub("ordering/place_an_take_away_order"))
        // @formatter:on
    }

}
