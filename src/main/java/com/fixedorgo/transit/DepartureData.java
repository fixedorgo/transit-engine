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
import com.fixedorgo.transit.DepartureData.DepartureApi.DepartureWas;
import com.fixedorgo.transit.DepartureData.DepartureApi.GetDepartureFor;
import com.fixedorgo.transit.DepartureData.DepartureApi.SetDepartureFor;
import com.google.common.collect.Maps;

import java.util.Map;

public class DepartureData extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final Map<String, Long> departureTime = Maps.newHashMap();

    @Override
    public void onReceive(Object message) throws Exception {

        log.debug("Received message [{}]", message);

        if (message instanceof SetDepartureFor) {

            final SetDepartureFor departureFor = (SetDepartureFor) message;
            departureTime.put(departureFor.stationId, System.currentTimeMillis());

        } else if (message instanceof GetDepartureFor) {

            final GetDepartureFor departureFor = (GetDepartureFor) message;
            final Long time = departureTime.get(departureFor.stationId);
            getSender().tell(new DepartureWas(departureFor.stationId, (time != null ? time : 0)), getSelf());

        } else {
            unhandled(message);
        }

    }

    public static class DepartureApi {

        public static class SetDepartureFor {
            public final String stationId;

            public SetDepartureFor(String stationId) {
                this.stationId = stationId;
            }
        }

        public static class GetDepartureFor {
            public final String stationId;

            public GetDepartureFor(String stationId) {
                this.stationId = stationId;
            }
        }

        public static class DepartureWas {
            public final String stationId;
            public final long departureTime;

            public DepartureWas(String stationId, long departureTime) {
                this.stationId = stationId;
                this.departureTime = departureTime;
            }
        }

    }

}
