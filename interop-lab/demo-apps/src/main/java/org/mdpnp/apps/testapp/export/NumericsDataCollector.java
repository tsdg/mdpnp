package org.mdpnp.apps.testapp.export;

import ice.Numeric;
import ice.Patient;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ListChangeListener;
import javafx.util.Callback;
import org.mdpnp.apps.fxbeans.ElementObserver;
import org.mdpnp.apps.fxbeans.NumericFx;
import org.mdpnp.apps.fxbeans.NumericFxList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class NumericsDataCollector extends DataCollector<NumericFx> {

    private static final Logger log = LoggerFactory.getLogger(NumericsDataCollector.class);

    private final NumericFxList numericList;
    private ElementObserver<NumericFx> numericObserver;

    @Override
    public void add(NumericFx fx) {
        try {
            if (log.isTraceEnabled())
                log.trace(dateFormats.get().format(fx.getPresentation_time()) + " " + fx.getMetric_id() + "=" + fx.getValue());
            Value v = toValue(fx);
            Patient patient = resolvePatient(v.getUniqueDeviceIdentifier());
            NumericSampleEvent ev = new NumericSampleEvent(patient, v);
            fireDataSampleEvent(ev);
        } catch (Exception e) {
            log.error("firing data sample event", e);
        }
    }

    public NumericsDataCollector(NumericFxList numericList) {

        this.numericList = numericList;
        this.numericObserver = new ElementObserver<>(numericExtractor, numericListenerGenerator, numericList);
        this.numericList.addListener(numericListener);
        this.numericList.forEach((fx)->numericObserver.attachListener(fx));
    }

    @Override
    public void destroy() {

        numericList.removeListener(numericListener);
        numericList.forEach((fx)->numericObserver.detachListener(fx));
    }

    private final ListChangeListener<NumericFx> numericListener = new ListChangeListener<NumericFx>() {
        @Override
        public void onChanged(javafx.collections.ListChangeListener.Change<? extends NumericFx> c) {
            while(c.next()) {
                if(c.wasAdded()) c.getAddedSubList().forEach((fx) -> numericObserver.attachListener(fx));
                if(c.wasRemoved()) c.getRemoved().forEach((fx) -> numericObserver.detachListener(fx));
            }
        }
    };

    private static final Callback<NumericFx, Observable[]> numericExtractor = new Callback<NumericFx, Observable[]>() {

        @Override
        public Observable[] call(NumericFx param) {
            return new Observable[] {
                    param.presentation_timeProperty()
            };
        }
    };


    private final Callback<NumericFx, InvalidationListener> numericListenerGenerator = new Callback<NumericFx, InvalidationListener>() {

        @Override
        public InvalidationListener call(final NumericFx param) {
            return new InvalidationListener() {

                @Override
                public void invalidated(Observable observable) {
                    add(param);
                }

            };
        }
    };

    @SuppressWarnings("serial")
    public static class NumericSampleEvent extends DataCollector.DataSampleEvent {

        private final Value data;

        public NumericSampleEvent(Value data) {
            this(UNDEFINED, data);
        }

        public NumericSampleEvent(Patient p, Value v) {
            super(p);
            data = v;
        }

        public Value getValue() {
            return data;
        }
    }
}