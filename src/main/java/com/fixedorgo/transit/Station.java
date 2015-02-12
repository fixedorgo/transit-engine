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
import com.fixedorgo.transit.Station.StationApi.Arrived;
import com.fixedorgo.transit.Station.StationApi.Boarding;
import com.fixedorgo.transit.Station.StationApi.StationData;
import com.fixedorgo.transit.Station.StationApi.ToAlight;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.javadocmd.simplelatlng.LatLng;
import org.uncommons.maths.number.NumberGenerator;
import org.uncommons.maths.random.ExponentialGenerator;
import org.uncommons.maths.random.MersenneTwisterRNG;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import static com.fixedorgo.transit.SystemClock.recalculateTime;
import static com.fixedorgo.transit.Bus.BusApi.Alighting;
import static com.fixedorgo.transit.Bus.BusApi.ToBoard;
import static com.fixedorgo.transit.Station.StationApi.Data;
import static com.fixedorgo.transit.Station.StationApi.PassengerHasArrived;
import static com.google.common.collect.Iterables.tryFind;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.time.DateUtils.MILLIS_PER_MINUTE;
import static org.apache.commons.lang3.time.DateUtils.MILLIS_PER_SECOND;

public class Station extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final String id;

    private final String name;

    private final LatLng location;

    private double arrivalRate;

    private final List<Passenger> stationQueue = Lists.newLinkedList();

    private final Set<String> currentlyServe = Sets.newHashSet();

    private long initialDelay = 10 * MILLIS_PER_SECOND; // Just example ???

    private NumberGenerator<Double> generator = new ExponentialGenerator(arrivalRate, new MersenneTwisterRNG());

    public Station(String id, String name, LatLng location, double arrivalRate) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.arrivalRate = arrivalRate;
    }

    @Override
    public void preStart() throws Exception {
        // schedule first passenger arriving
        context().system().scheduler().scheduleOnce(Duration.create(recalculateTime(initialDelay), MILLISECONDS),
                getSelf(), PassengerHasArrived, getContext().dispatcher(), ActorRef.noSender());
    }

    @Override
    public void onReceive(Object message) throws Exception {

        log.debug("Received message [{}]", message);

        if (message.equals(PassengerHasArrived)) {
            // send another periodic passenger after the specified delay
            context().system().scheduler().scheduleOnce(Duration.create(recalculateTime(interval()), MILLISECONDS),
                    getSelf(), PassengerHasArrived, getContext().dispatcher(), ActorRef.noSender());

            // First: if necessary bus already on station passenger goes to boarding
            // Second: if there are no buses passenger should be added to Station queue
            stationQueue.add(new Passenger("", "", Lists.newArrayList("", ""))); // TODO: Add Passenger generator

        } else if (message instanceof Arrived) {

            final Arrived arrived = (Arrived) message;
            currentlyServe.add(arrived.routeId); // TODO: Add some statistics here?
            getSender().tell(new Alighting(id), getSelf());

        } else if (message instanceof ToAlight) {

            final ToAlight toAlight = (ToAlight) message;
            Passenger passenger = toAlight.passenger; // TODO: Where should go this Passenger?
            getSender().tell(new Alighting(id), getSelf());

        } else if (message instanceof Boarding) {

            final Boarding boarding = (Boarding) message;
            Optional<Passenger> candidate = tryFind(stationQueue, toBoard(boarding.routeId));
            if (candidate.isPresent()) {
                Passenger passenger = candidate.get();
                stationQueue.remove(passenger);

                // Simulate the boarding time
                context().system().scheduler().scheduleOnce(Duration.create(recalculateTime(boarding.time), MILLISECONDS),
                        getSender(), new ToBoard(passenger), getContext().dispatcher(), getSelf());
            } else {
                getSender().tell(new ToBoard(Passenger.NONE), getSelf()); // TODO: Replace by scheduleOnce() ???
            }

        } else if (message.equals(Data)) {

            // sending of Station Data to sender
            getSender().tell(new StationData(id, name, location), getSelf());

        } else {
            unhandled(message);
        }

    }

    private long interval() {
        return Math.round(generator.nextValue() * MILLIS_PER_MINUTE);
    }

    private Predicate<Passenger> toBoard(final String routeId) {
        return new Predicate<Passenger>() {
            @Override
            public boolean apply(Passenger passenger) {
                return passenger.isSuitable(routeId);
            }
        };
    }

    public static class StationApi {

        public static final Object PassengerHasArrived = "Passenger Has Arrived";
        public static final Object Data = "Data";

        public static class Arrived implements Serializable {
            public final String busId;
            public final String routeId;

            public Arrived(String busId, String routeId) {
                this.busId = busId;
                this.routeId = routeId;
            }
        }

        public static class Boarding implements Serializable {

            public final String routeId;
            public final long time;
            public final int load;

            public Boarding(String routeId, long time, int load) {
                this.routeId = routeId;
                this.time = time;
                this.load = load;
            }

            @Override
            public String toString() {
                return String.format("[%s, %s, %s]", routeId, time, load);
            }

        }

        public static class ToAlight implements Serializable {
            public final Passenger passenger;

            public ToAlight(Passenger passenger) {
                this.passenger = passenger;
            }

            @Override
            public String toString() {
                return passenger.toString();
            }
        }

        public static class StationData implements Serializable {

            public final String id;

            public final String name;

            public final LatLng location;

            public StationData(String id, String name, LatLng location) {
                this.id = id;
                this.name = name;
                this.location = location;
            }

            @Override
            public String toString() {
                return String.format("Station [%s, %s, %s]", id, name, location);
            }

        }

    }

}
