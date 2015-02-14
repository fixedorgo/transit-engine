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
import com.fixedorgo.transit.Tracking.TrackingApi.Track;
import com.google.common.collect.Maps;
import com.javadocmd.simplelatlng.LatLng;

import java.io.Serializable;
import java.util.Map;

public class Tracking extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final Map<Long, LatLng> movingTrack = Maps.newLinkedHashMap();

    private final Map<Long, Double> totalPath = Maps.newLinkedHashMap();

    private double distance = 0;

    @Override
    public void preStart() throws Exception {
        totalPath.put(System.currentTimeMillis(), distance); // ???
    }

    @Override
    public void onReceive(Object message) throws Exception {

        log.debug("Received message [{}]", message);

        if (message instanceof Track) {

            final Track track = (Track) message;
            final Point point = track.point;

            movingTrack.put(System.currentTimeMillis(), point.getLatLng());
            totalPath.put(System.currentTimeMillis(), distance += point.getDistance());

        } else {
            unhandled(message);
        }

    }

    public static class TrackingApi {

        public static class Track implements Serializable {
            public final Point point;

            public Track(Point point) {
                this.point = point;
            }
        }

    }

}
