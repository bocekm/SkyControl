/*
 * Copyright (c) 2014, Michal Bocek, All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.bocekm.skycontrol;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;

/**
 * Simple {@link MapView} wrapper which adds listener on user touch on map. In that case onMapTouch
 * is called.
 */
public class ListeningMapView extends MapView {

    /**
     * The listener interface for receiving ListeningMapView events. The class that is interested in
     * processing a ListeningMapView event implements this interface, and the object created with
     * that class is registered with a component using the component's
     * <code>registerMapViewListener</code> method. When
     * the ListeningMapView event occurs, that object's appropriate
     * method is invoked.
     */
    public interface MapViewListener {

        /**
         * Gets called when user touches the map.
         */
        public void onMapTouch();
    }

    /** Registered listener. */
    MapViewListener mListener = null;

    /*
     * (non-Javadoc) Notifies the registered listener about the touch on map event.
     * 
     * @see android.view.ViewGroup#dispatchTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mListener != null)
                    mListener.onMapTouch();
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * Register listener to the map touch event.
     * 
     * @param listener the listener
     */
    public void registerMapViewListener(MapViewListener listener) {
        mListener = listener;
    }

    /**
     * Unregister listener to the map touch event.
     */
    public void unregisterMapViewListener() {
        mListener = null;
    }

    /**
     * Calls the superclass constructor.
     * 
     * @param context the context
     */
    public ListeningMapView(Context context) {
        super(context);
    }

    /**
     * Calls the superclass constructor.
     * 
     * @param context the context
     * @param attrs the attrs
     */
    public ListeningMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Calls the superclass constructor.
     * 
     * @param context the context
     * @param options the options
     */
    public ListeningMapView(Context context, GoogleMapOptions options) {
        super(context, options);
    }

    /**
     * Calls the superclass constructor.
     * 
     * @param context the context
     * @param attrs the attrs
     * @param defStyle the def style
     */
    public ListeningMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

}
