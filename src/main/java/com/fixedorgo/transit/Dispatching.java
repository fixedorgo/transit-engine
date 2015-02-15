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
import com.fixedorgo.transit.Route.RouteApi.RouteData;
import com.fixedorgo.transit.Dispatching.DispatchingApi.GetRouteData;
import com.google.common.collect.ImmutableList;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Dispatching extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final Map<String, List<String>> stations;

    private final Map<String, String> reverseRoutes;

    public Dispatching(Map<String, List<String>> stations, Map<String, String> reverseRoutes) {
        this.stations = stations;
        this.reverseRoutes = reverseRoutes;
    }

    @Override
    public void onReceive(Object message) throws Exception {

        log.debug("Received message [{}]", message);

        if (message instanceof GetRouteData) {
            final GetRouteData routeData = (GetRouteData) message;
            final String routeId = routeData.reverseRoute ? reverseRoute(routeData.routeId) : routeData.routeId;
            getSender().tell(new RouteData(routeId, stationsFor(routeId)), getSelf());
        } else {
            unhandled(message);
        }

    }

    private List<String> stationsFor(String routeId) {
        if (!stations.containsKey(routeId))
            throw new IllegalArgumentException(String.format("Unable to find Stations for Route [%s]", routeId));
        return ImmutableList.copyOf(stations.get(routeId));
    }

    private String reverseRoute(String routeId) {
        if (!reverseRoutes.containsKey(routeId))
            throw new IllegalArgumentException(String.format("Unable to find reverse Route for Route [%s]", routeId));
        return reverseRoutes.get(routeId);
    }

    public static class DispatchingApi {

        public static class GetRouteData implements Serializable {
            public final String routeId;
            public final boolean reverseRoute;

            public GetRouteData(String routeId, boolean reverseRoute) {
                this.routeId = routeId;
                this.reverseRoute = reverseRoute;
            }
        }

    }

}
