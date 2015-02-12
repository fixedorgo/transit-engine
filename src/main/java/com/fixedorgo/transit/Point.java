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

import com.javadocmd.simplelatlng.LatLng;

public class Point {

    private long distance;

    private long duration;

    private LatLng latLng;

    public Point(long distance, long duration, LatLng latLng) {
        this.distance = distance;
        this.duration = duration;
        this.latLng = latLng;
    }

    public long getDistance() {
        return distance;
    }

    public long getDuration() {
        return duration;
    }

    public LatLng getLatLng() {
        return latLng;
    }

}
