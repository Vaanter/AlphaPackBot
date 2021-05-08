/*
 *    Copyright 2020 Valentín Bolfík
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.vb.alphapackbot;


import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.ShutdownEvent;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AdminService extends AdminGrpc.AdminImplBase {
  private static final Properties properties = Properties.getInstance();
  final Telemetry telemetry;
  final Event<ShutdownEvent> event;
  final Cache cache;
  final EventBus bus;

  @Inject
  AdminService(final Telemetry telemetry,
               final Event<ShutdownEvent> event,
               final Cache cache,
               final EventBus bus) {
    this.telemetry = telemetry;
    this.event = event;
    this.cache = cache;
    this.bus = bus;
  }

  @Override
  public void getStatus(final StatusRequest request,
                        final StreamObserver<StatusReply> responseObserver) {
    responseObserver.onNext(StatusReply
        .newBuilder()
        .setUptime(telemetry.formatUptime())
        .setCommandsReceived(telemetry.getCommandsReceived().longValue())
        .setIsBotEnabled(properties.isBotEnabled())
        .setProcessingCounter(properties.getProcessingCounter().intValue())
        .setIsCacheAvailable(cache.isAvailable())
        .setIsCacheEnabled(properties.isCacheEnabled())
        .setIsPrintingEnabled(properties.isPrintingEnabled()).build());
    responseObserver.onCompleted();
  }

  @Override
  public void toggleProperty(final ToggleRequest request,
                             final StreamObserver<ToggleResponse> responseObserver) {
    switch (request.getToggle()) {
      case BOT:
        properties.setBotEnabled(request.getNewValue());
        break;
      case PRINTING:
        properties.setPrintingEnabled(request.getNewValue());
        break;
      case CACHE:
        properties.setCacheEnabled(request.getNewValue());
        break;
      default:
        break;
    }
    responseObserver.onNext(ToggleResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void setBotStatus(final BotStatusRequest request,
                           final StreamObserver<BotStatusReply> responseObserver) {
    bus.<Status>request("set-activity", request)
        .onItem()
        .transform(Message::body)
        .subscribe()
        .with(status -> {
          responseObserver.onNext(
              BotStatusReply
                  .newBuilder()
                  .setStatusCode(status.getCode().value())
                  .build());
          responseObserver.onCompleted();
        });
  }

  @Override
  public void exit(final ExitRequest request,
                   final StreamObserver<ExitResponse> responseObserver) {
    responseObserver.onNext(ExitResponse.newBuilder().build());
    responseObserver.onCompleted();
    event.fire(new ShutdownEvent());
    Quarkus.asyncExit();
  }
}
