# ftx4j
[![Build Status][travis-icon]][travis-page]

*ftx4j* is a Java wrapper library for the [FTX][ftx-home]'s FIX client API.

This library aims to provide request/response interfaces for creating/cancelling orders, 
capsulizing the asynchronous nature of FIX protocol with the Java's standard `CompletableFuture` class.
Asynchronous events, such as order updates and executions, are accessible via `Ftx4jListener` callback interface.

## Prerequisites
* Java Development Kit 11
* Apache Maven / Gradle Build Tool

## Repository

```xml
<repositories>
    <repository>
        <id>sonatype-snapshot</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```
```xml
<dependencies>
    <dependency>
        <groupId>com.after_sunrise.api</groupId>
        <artifactId>ftx4j</artifactId>
        <version>${latest}-SNAPSHOT</version>
    </dependency>
</dependencies>
```

## Usage

Below is a snippet from [Ftx4jApiTest][src-test].

```java
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
```

## Configurations

Following properties are required, all else are optional.
* `Ftx4jConfig.AUTH_APIKEY`
* `Ftx4jConfig.AUTH_SECRET`

Refer to [Ftx4jConfig][src-conf] for the list of available configurations and descriptions.

## Notes / Disclaimers

* No support, no guarantee. Use at your own risk.
* Rate limit (per sub-account) is shared with the REST API.
* FIX connection (logon) is per account. Create multiple API instances for multiple sub-accounts.
* What's implemented:
  * Logon (A)
  * Heartbeat (0)
  * Test Request (1)
  * Logout (5)
  * New Order Single (D)
  * Order Cancel Request (F)
  * Order Cancel Reject (9)
  * Order Status Request (H) (IncludeFillInfo=N)
  * Execution Report (8)
  * Reject (3)
* What's NOT implemented:
  * Mass Order Cancel Request (q)
  * Mass Order Cancel Report (r)
  * Order Status Request (H) (IncludeFillInfo=Y)

[travis-page]:https://travis-ci.org/after-the-sunrise/ftx4j
[travis-icon]:https://travis-ci.org/after-the-sunrise/ftx4j.svg?branch=master
[ftx-home]:https://ftx.com/#a=ftx4j
[src-test]:https://github.com/after-the-sunrise/ftx4j/blob/master/src/test/java/com/after_sunrise/api/ftx4j/Ftx4jApiTest.java
[src-conf]:https://github.com/after-the-sunrise/ftx4j/blob/master/src/main/java/com/after_sunrise/api/ftx4j/Ftx4jConfig.java
