/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.core.control;

import org.apache.karaf.cellar.core.command.Result;

/**
 * Producer switch result.
 */
public class ProducerSwitchResult extends Result {

    protected Boolean sucess = Boolean.TRUE;
    protected Boolean status = Boolean.TRUE;

    /**
     * Constructor
     *
     * @param id
     */
    public ProducerSwitchResult(String id) {
        super(id);
    }

    /**
     * Constructor
     *
     * @param id
     * @param sucess
     */
    public ProducerSwitchResult(String id, Boolean sucess) {
        super(id);
        this.sucess = sucess;
    }

    /**
     * Constructor
     *
     * @param id
     * @param sucess
     * @param status
     */
    public ProducerSwitchResult(String id, Boolean sucess, Boolean status) {
        super(id);
        this.sucess = sucess;
        this.status = status;
    }

    public Boolean getSucess() {
        return sucess;
    }

    public void setSucess(Boolean sucess) {
        this.sucess = sucess;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

}
