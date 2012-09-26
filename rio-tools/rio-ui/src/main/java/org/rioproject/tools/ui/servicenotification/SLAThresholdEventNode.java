/*
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.tools.ui.servicenotification;

import org.rioproject.sla.SLA;
import org.rioproject.sla.SLAThresholdEvent;
import org.rioproject.tools.ui.Constants;
import org.rioproject.watch.ThresholdEvent;

import java.text.NumberFormat;

/**
 * @author Dennis Reedy
 */
public class SLAThresholdEventNode extends RemoteServiceEventNode<SLAThresholdEvent> {
    private final NumberFormat numberFormatter;

    public SLAThresholdEventNode(SLAThresholdEvent event) {
        super(event);
        numberFormatter = NumberFormat.getNumberInstance();
        numberFormatter.setGroupingUsed(false);
        numberFormatter.setMaximumFractionDigits(2);
    }

    @Override
    public Throwable getThrown() {
        return null;
    }

    @Override
    public String getDescription() {
        StringBuilder builder = new StringBuilder();
        SLA sla = getEvent().getSLA();
        builder.append(getEvent().getServiceElement().getName()).append(" on ");
        builder.append(getEvent().getHostAddress()).append(" SLA ");
        builder.append("\"").append(sla.getIdentifier()).append("\"").append(" ");
        builder.append(getStatus().toLowerCase()).append(", ");
        builder.append("value: ").append(numberFormatter.format(getEvent().getCalculable().getValue()));
        return builder.toString();
    }

    @Override
    public String getOperationalStringName() {
        return getEvent().getServiceElement().getOperationalStringName();
    }

    @Override
    public String getServiceName() {
        return getEvent().getServiceElement().getName();
    }

    public String getStatus() {
        return getEvent().getType() == ThresholdEvent.BREACHED?"BREACHED":"CLEARED";
    }

    @Override
    public String toString() {
        return getServiceName();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int column) {
        String value;
        if (column == 0) {
            value = getStatus();
        } else if (column == 1) {
            value = getDescription();
        } else {
            value = Constants.DATE_FORMAT.format(getDate());
        }
        return value;
    }
}

