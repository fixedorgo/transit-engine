/*
 * Copyright (C) 2015 Timur Zagorsky
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fixedorgo.transit;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.fixedorgo.transit.Bus.BusApi.Ready;
import com.fixedorgo.transit.Dispatching.DispatchingApi.GetRouteData;
import com.fixedorgo.transit.Route.RouteApi.RouteData;
import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.List;
import java.util.Queue;

import static com.fixedorgo.transit.Bus.BusApi.NextStation;
import static com.fixedorgo.transit.Route.RouteApi.GetNextStation;

public class Route extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private String routeId;

    private final Queue<String> route = Lists.newLinkedList();

    public Route(String routeId) {
        this.routeId = routeId;
    }

    @Override
    public void preStart() throws Exception {
        getContext().actorSelection("/user/dispatching").tell(new GetRouteData(routeId, false), getSelf());
    }

    @Override
    public void onReceive(Object message) throws Exception {
        log.debug("Received message [{}]", message);

        if (message instanceof RouteData) {

            final RouteData routeData = (RouteData) message;
            routeId = routeData.routeId;
            route.addAll(routeData.stations);
            getContext().parent().tell(new Ready(routeId), getSelf()); // TODO: Just a Stub at this time

        } else if (message.equals(GetNextStation)) {

            if (!route.isEmpty())
                getSender().tell(new NextStation(route.poll(), route.isEmpty()), getSelf()); // TODO: Should we mark as final?
            else
                getContext().actorSelection("/user/dispatching").tell(new GetRouteData(routeId, true), getSelf());

        } else {
            unhandled(message);
        }

    }

    public static class RouteApi {

        public static final Object GetNextStation = "Get Next Station";
        public static final Object FinalStation = "Final Station"; // ???

        public static class RouteData implements Serializable {
            public final String routeId;
            public final List<String> stations;

            public RouteData(String routeId, List<String> stations) {
                this.routeId = routeId;
                this.stations = stations;
            }
        }

    }

}
