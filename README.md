# WsManager

A library that simplifies the use of OkHttp Websocket.

For more information, please see:

https://rabtman.com/2017/01/21/okhttp_ws_use/

https://rabtman.com/2017/01/28/okhttp_ws_source/

## Download

Maven:

```xml
<dependency>
  <groupId>com.rabtman.wsmanager</groupId>
  <artifactId>wsmanager</artifactId>
  <version>1.0.2</version>
  <type>pom</type>
</dependency>
```

or Gradle:

```groovy
compile 'com.rabtman.wsmanager:wsmanager:1.0.2'
```

## How to use

Instantiate a WsManager object:

```
OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                      .pingInterval(15, TimeUnit.SECONDS)
                      .retryOnConnectionFailure(true)
                      .build();
WsManager wsManager = new WsManager.Builder(this)
                .wsUrl("ws://localhost:2333/")
                .needReconnect(true)
                .client(okHttpClient)
                .build();
```

Establish a connection with the server:

```
wsManager.startConnect();
```

Listens for server connection status:

```
wsManager.setWsStatusListener(new WsStatusListener() {
            @Override
            public void onOpen(Response response) {
                super.onOpen(response);
            }

            @Override
            public void onMessage(String text) {
                super.onMessage(text);
            }

            @Override
            public void onMessage(ByteString bytes) {
                super.onMessage(bytes);
            }

            @Override
            public void onReconnect() {
                super.onReconnect();
            }

            @Override
            public void onClosing(int code, String reason) {
                super.onClosing(code, reason);
            }

            @Override
            public void onClosed(int code, String reason) {
                super.onClosed(code, reason);
            }

            @Override
            public void onFailure(Throwable t, Response response) {
                super.onFailure(t, response);
            }
        });
```

Send message to the server:

```
//String msg or ByteString byteString
wsManager.sendMessage();
```

Close the connection to the server:

```
wsManager.stopConnect();
```

## Preview

![](https://github.com/Rabtman/WsManager/raw/master/screenshots/ws.gif)

## License

```
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
