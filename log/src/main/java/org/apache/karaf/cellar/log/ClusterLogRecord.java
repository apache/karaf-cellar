/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.log;

import java.io.Serializable;

public class ClusterLogRecord implements Serializable {

    private String level;
    private String loggerName;
    private String message;
    private String renderedMessage;
    private String threadName;
    private String FQNOfLoggerClass;
    private String[] throwableStringRep;

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRenderedMessage() {
        return renderedMessage;
    }

    public void setRenderedMessage(String renderedMessage) {
        this.renderedMessage = renderedMessage;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getFQNOfLoggerClass() {
        return FQNOfLoggerClass;
    }

    public void setFQNOfLoggerClass(String FQNOfLoggerClass) {
        this.FQNOfLoggerClass = FQNOfLoggerClass;
    }

    public String[] getThrowableStringRep() {
        return throwableStringRep;
    }

    public void setThrowableStringRep(String[] throwableStringRep) {
        this.throwableStringRep = throwableStringRep;
    }

}
