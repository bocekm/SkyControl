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
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

/**
 * Provides a dialog used for setting the number from user preferences. Set unit(optional), min and
 * max value by using skycontrol:unit, skycontrol:minValue, skycontrol:maxValue. Namespace in
 * preferences.xml should be set to
 * xmlns:skycontrol="http://schemas.android.com/apk/res/com.bocekm.skycontrol".
 */
public class NumberPickerPreference extends DialogPreference {

    /** Reference to number picker object inflated in dialog. */
    private NumberPicker mNumberPicker;

    /** Current value of the number picker. */
    private Integer mCurrentValue = null;

    /** Minimum value pickable in the number picker. */
    private int mMinValue;
    /** Maximum value pickable in the number picker. */
    private int mMaxValue;
    
    /** Optional string representing unit of the value. */
    private String mUnit = null;

    /**
     * Used only if the default value isn't set in preference XML using android:defaultValue
     * attribute.
     */
    private static final int DEFAULT = 0;

    /**
     * Constructor that is called when inflating a Preference from XML. This is called when a
     * Preference is being constructed from an XML file, supplying attributes that were specified in
     * the XML file.
     * 
     * @param context the Context this is associated with, through which it can access the current
     *        theme, resources, SharedPreferences, etc.
     * @param attrs the attributes of the XML tag that is inflating the preference.
     */
    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray styled =
                getContext().obtainStyledAttributes(attrs, R.styleable.NumberPickerPreference);
        try {
            // Retrieve the values from an XML
            mMinValue = styled.getInt(R.styleable.NumberPickerPreference_minValue, 0);
            mMaxValue = styled.getInt(R.styleable.NumberPickerPreference_maxValue, 1);
            mUnit = styled.getString(R.styleable.NumberPickerPreference_unit);
        } finally {
            styled.recycle();
        }

        // Retrieve the dialog title from an XML
        int titleResource =
                attrs.getAttributeResourceValue("http://schemas.android.com/apk/res/android",
                        "title", 0);

        mUnit = mUnit != null ? (" " + mUnit) : "";
        setDialogTitle(context.getString(titleResource) + " (max " + mMaxValue + mUnit + ")");

        setDialogLayoutResource(R.layout.number_picker_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);
    }

    /*
     * (non-Javadoc) Set default value from the XML attribute
     * 
     * @see android.preference.Preference#onGetDefaultValue(android.content.res.TypedArray, int)
     */
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, DEFAULT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.preference.DialogPreference#onCreateDialogView()
     */
    @Override
    protected View onCreateDialogView() {
        View view = super.onCreateDialogView();
        mCurrentValue = getPersistedInt(DEFAULT);

        mNumberPicker = (NumberPicker) view.findViewById(R.id.number_picker);

        // Initialize parameters of number picker
        mNumberPicker.setMinValue(mMinValue);
        mNumberPicker.setMaxValue(mMaxValue);
        mNumberPicker.setValue(mCurrentValue);
        mNumberPicker.setWrapSelectorWheel(false);

        return view;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.preference.Preference#onSetInitialValue(boolean, java.lang.Object)
     */
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing value
            mCurrentValue = getPersistedInt(DEFAULT);
        } else {
            // Set default value from the XML attribute
            mCurrentValue = (Integer) defaultValue;
            persistInt(mCurrentValue);
        }
    }

    /*
     * (non-Javadoc) Save the value set by user.
     * 
     * @see android.preference.DialogPreference#onDialogClosed(boolean)
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            persistInt(mNumberPicker.getValue());
        }
    }
}
