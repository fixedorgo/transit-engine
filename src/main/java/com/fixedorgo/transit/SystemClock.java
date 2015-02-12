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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static java.lang.Math.max;

public class SystemClock {

    private static Logger log = LoggerFactory.getLogger(SystemClock.class);

    private static int timeScale = 1;

    public static long recalculateTime(long time) {
        return max(time, 0) / timeScale;
    }

    public static void setTimeScale(int timeScale) {
        SystemClock.timeScale = timeScale;
    }

    public static void waitFor(long duration, TimeUnit unit) {
        try {
            unit.sleep(recalculateTime(duration));
        } catch (InterruptedException e) {
            log.debug("Waiting was interrupted", e);
        }
    }

}
