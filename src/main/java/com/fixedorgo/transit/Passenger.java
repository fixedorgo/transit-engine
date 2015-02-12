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

import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class Passenger {

    // Passenger with id = 0
    public static final Passenger NONE = new Passenger(EMPTY, EMPTY, Collections.<String>emptyList());

    private static int counter = 0;

    private final int id = counter++;

    private String origin;

    private String destination;

    private List<String> suitableRoutes;

    public Passenger(String origin, String destination, List<String> suitableRoutes) {
        this.origin = origin;
        this.destination = destination;
        this.suitableRoutes = suitableRoutes;
    }

    public boolean isSuitable(String routeId) {
        return suitableRoutes.contains(routeId); // simple case. A Decision Strategy is needed
    }

    public boolean hasDestination(String stationId) {
        return destination.equals(stationId);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Passenger &&
                id == Passenger.class.cast(obj).id;
    }

    @Override
    public String toString() {
        return String.format("Passenger [%s]", id);
    }

}
