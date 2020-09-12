package com.after_sunrise.api.ftx4j;

import com.after_sunrise.api.ftx4j.entity.Ftx4jCancelResponse;
import com.after_sunrise.api.ftx4j.entity.Ftx4jCreateResponse;
import com.after_sunrise.api.ftx4j.entity.Ftx4jExecInstType;
import com.after_sunrise.api.ftx4j.entity.Ftx4jIdType;
import com.after_sunrise.api.ftx4j.entity.Ftx4jSideType;
import com.after_sunrise.api.ftx4j.entity.Ftx4jTimeInForceType;
import com.after_sunrise.api.ftx4j.entity.ImmutableFtx4jCancelRequest;
import com.after_sunrise.api.ftx4j.entity.ImmutableFtx4jCreateRequest;

import java.math.BigDecimal;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

class Ftx4jApiTest {

    public static void main(String[] args) throws Exception {

        Properties properties = new Properties(System.getProperties());
        properties.setProperty(Ftx4jConfig.AUTH_APIKEY.getId(), "foo");
        properties.setProperty(Ftx4jConfig.AUTH_SECRET.getId(), "bar");
        properties.setProperty(Ftx4jConfig.FIX_CANCEL_ON_DISCONNECT.getId(), "S");

        CompletableFuture<Ftx4jSession> future = new CompletableFuture<>();

        Ftx4jListener listener = new Ftx4jListener() {
            @Override
            public void onConnect(Ftx4jSession session) {
                future.complete(session); // logon completed
            }
        };

        //
        // Initialize API instance. (& terminate when finished.)
        //
        try (Ftx4jApi api = Ftx4jApi.create(properties, listener)) {

            //
            // Await for successful logon.
            //
            Ftx4jSession session = future.get(42, TimeUnit.SECONDS);
            System.out.println("Session : " + session);
            TimeUnit.SECONDS.sleep(5);

            //
            // Create order.
            //
            CompletableFuture<Ftx4jCreateResponse> create = api.createOrder(ImmutableFtx4jCreateRequest.builder()
                    .session(session)
                    .symbol("ETH-PERP")
                    .side(Ftx4jSideType.BUY)
                    .size(new BigDecimal("0.01"))
                    .price(new BigDecimal("123.0"))
                    .timeInForce(Ftx4jTimeInForceType.GOOD_TIL_CANCEL)
                    .execInst(Ftx4jExecInstType.POST_ONLY)
                    .build()
            );
            System.out.println("Create : " + create.get(5, TimeUnit.SECONDS));
            TimeUnit.SECONDS.sleep(15);

            //
            // Cancel order.
            //
            CompletableFuture<Ftx4jCancelResponse> cancel = api.cancelOrder(ImmutableFtx4jCancelRequest.builder()
                    .session(session)
                    .idType(Ftx4jIdType.SYSTEM)
                    .idValue(create.get().getOrder().getOrderId())
                    .build()
            );
            System.out.println("Cancel : " + cancel.get(5, TimeUnit.SECONDS));
            TimeUnit.SECONDS.sleep(5);

        }

    }

}
