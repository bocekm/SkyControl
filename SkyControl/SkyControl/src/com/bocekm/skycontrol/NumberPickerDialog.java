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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;

/**
 * Provides a dialog used for setting a number.
 */
public class NumberPickerDialog extends DialogFragment {

    /** Tag representing the returning picked number. */
    public static final String EXTRA_PICKED_NUMBER = "com.bocekm.skycontrol.picked_number";
    /** Arguments tag representing maximum number to show in number picker. */
    public static final String EXTRA_MAX_VALUE = "com.bocekm.skycontrol.max_value";    
    /** Arguments tag representing minimum number to show in number picker. */
    public static final String EXTRA_MIN_VALUE = "com.bocekm.skycontrol.min_value";
    /** Arguments tag representing the dialog title. */
    public static final String EXTRA_DIALOG_TITLE = "com.bocekm.skycontrol.dialog_title";
    /** The picked number. */
    private int mPickedNumber;

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.number_picker_dialog, null);

        // Retrieve value to set in number picker from dialog arguments
        mPickedNumber = getArguments().getInt(EXTRA_PICKED_NUMBER);
        NumberPicker numberPicker = (NumberPicker) view.findViewById(R.id.number_picker);
        numberPicker.setMinValue(getArguments().getInt(EXTRA_MIN_VALUE, 0));
        numberPicker.setMaxValue(getArguments().getInt(EXTRA_MAX_VALUE, 0));
        numberPicker.setValue(mPickedNumber);
        numberPicker.setWrapSelectorWheel(false);
        numberPicker.setOnValueChangedListener(new OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                mPickedNumber = newVal;
                // Update argument to preserve selected value on configuration change
                getArguments().putInt(EXTRA_PICKED_NUMBER, newVal);
            }
        });

        // Build new dialog with OK and Cancel buttons
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getArguments().getString(EXTRA_DIALOG_TITLE, "Choose number")).setView(
                view);
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            /*
             * (non-Javadoc)
             * 
             * @see
             * android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface
             * , int)
             */
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                sendResult(Activity.RESULT_OK);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    /**
     * Send selected number back to the fragment initiating this dialog.
     * 
     * @param resultCode the result code
     */
    private void sendResult(int resultCode) {
        if (getTargetFragment() == null)
            return;
        Intent i = new Intent();
        i.putExtra(EXTRA_PICKED_NUMBER, mPickedNumber);
        getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, i);
    }

    /**
     * Instantiate new {@link NumberPickerDialog}. Use this method instead of constructor to have
     * the correct number picker init values.
     *
     * @param valueToSet the value to set in number picker
     * @param minValue the min value possible to choose from in number picker
     * @param maxValue the max value possible to choose from in number picker
     * @param dialogTitle title of the dialog
     * @return new instance of {@link NumberPickerDialog}
     */
    public static NumberPickerDialog newInstance(int valueToSet, int minValue, int maxValue, String dialogTitle) {
        // Save the init values to dialog arguments
        Bundle args = new Bundle();
        args.putInt(EXTRA_PICKED_NUMBER, valueToSet);
        args.putInt(EXTRA_MIN_VALUE, minValue);
        args.putInt(EXTRA_MAX_VALUE, maxValue);
        args.putString(EXTRA_DIALOG_TITLE, dialogTitle);

        NumberPickerDialog fragment = new NumberPickerDialog();
        fragment.setArguments(args);
        return fragment;
    }
}
