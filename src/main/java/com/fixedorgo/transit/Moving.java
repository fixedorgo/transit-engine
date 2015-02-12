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

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.fixedorgo.transit.Tracking.TrackingApi.Track;
import com.fixedorgo.transit.Bus.BusApi.Locate;
import com.fixedorgo.transit.Moving.MovingApi.Load;
import com.fixedorgo.transit.Moving.MovingApi.MoveTo;
import com.google.common.collect.Lists;
import com.javadocmd.simplelatlng.LatLng;
import org.uncommons.maths.number.NumberGenerator;
import org.uncommons.maths.random.ContinuousUniformGenerator;
import org.uncommons.maths.random.MersenneTwisterRNG;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.List;
import java.util.Queue;

import static com.fixedorgo.transit.SystemClock.recalculateTime;
import static com.fixedorgo.transit.Bus.BusApi.WeAreHere;
import static com.fixedorgo.transit.Moving.MovingApi.MoveOn;
import static com.fixedorgo.transit.Moving.MovingApi.Reached;
import static com.javadocmd.simplelatlng.LatLngTool.distance;
import static com.javadocmd.simplelatlng.util.LengthUnit.METER;
import static java.util.concurrent.TimeUnit.SECONDS;

public class Moving extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final NumberGenerator<Double> generator = new ContinuousUniformGenerator(0.9, 1.1, new MersenneTwisterRNG());

    private final Queue<Point> routePoints = Lists.newLinkedList();

    private LatLng destination;

    private ActorRef tracking;

    @Override
    public void preStart() throws Exception {
        // TODO: Initialize tracking
    }

    @Override
    public void onReceive(Object message) throws Exception {
        log.debug("Received message [{}]", message);

        if (message instanceof Load) {

            final Load load = (Load) message;
            routePoints.addAll(load.routePoints);

        } else if (message instanceof MoveTo) {

            final MoveTo moveTo = (MoveTo) message;
            destination = moveTo.destination;
            getSelf().tell(MoveOn, ActorRef.noSender());

        } else if (message.equals(MoveOn)) {

            final Point nextPoint = routePoints.peek(); // take but doesn't remove
            long duration = (long) (nextPoint.getDuration() * generator.nextValue());

            // moving simulation itself
            getContext().system().scheduler().scheduleOnce(Duration.create(recalculateTime(duration), SECONDS),
                    getSelf(), Reached, getContext().dispatcher(), ActorRef.noSender());

        } else if (message.equals(Reached)) {

            final Point currentPoint = routePoints.poll();
            final LatLng currentLocation = currentPoint.getLatLng();

            // tell the Bus about current location
            getContext().parent().tell(new Locate(currentLocation), getSelf());

            // path tracking activities
            tracking.tell(new Track(currentPoint), getSelf());

            if (weArrivedAt(currentLocation))
                getContext().parent().tell(WeAreHere, getSelf()); // TODO: Just a stub
            else
                getSelf().tell(MoveOn, ActorRef.noSender());

        } else {
            unhandled(message);
        }

    }

    private boolean weArrivedAt(LatLng location) {
        double toDestination = distance(destination, location, METER);
        double fromNextPoint = distance(destination, routePoints.peek().getLatLng(), METER);
        return toDestination < 10 && toDestination < fromNextPoint;
    }

    public static class MovingApi {

        public static final Object MoveOn = "Move On";
        public static final Object Reached = "Reached";

        public static class Load implements Serializable {
            public final List<Point> routePoints;

            public Load(List<Point> routePoints) {
                this.routePoints = routePoints;
            }
        }

        public static class MoveTo implements Serializable {
            public final LatLng destination;

            public MoveTo(LatLng destination) {
                this.destination = destination;
            }
        }

    }

}
