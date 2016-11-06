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
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * The Class AlertDialogFragment. Instantiate this class with
 * {@link AlertDialogFragment#newInstance(String, String)}. The class interested in OK button click
 * event shall implement AlertDialogListener.
 */
public class AlertDialogFragment extends DialogFragment {

    /** Use this instance of the interface to deliver action events */
    AlertDialogListener mListener;

    /**
     * Create a dialog with specific title and message.
     * 
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        // create new dialog with dialog Builder
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        // set dialog's title and message passed to a newInstance method
        alertDialog.setTitle(getStringArg("title"));
        alertDialog.setMessage(getStringArg("message"));
        alertDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // Send the positive button event back to the host activity
                mListener.onDialogPositiveClick(AlertDialogFragment.this);
            }
        });

        return alertDialog.create();
    }

    /**
     * Gets the String from {@link Bundle} passed by
     * {@link AlertDialogFragment#newInstance(String, String)}.
     * 
     * @param tag tag of argument to retrieve from argument Bundle
     * @return String content of argument denoted by tag
     */
    private String getStringArg(String tag) {
        String arg = getArguments().getString(tag, SkyControlConst.EMPTY_STRING);
        if (arg.equals(SkyControlConst.EMPTY_STRING))
            throw new Error(this.getClass().getSimpleName()
                    + " should get instantiated by newInstance method.");
        return arg;
    }

    /**
     * Creates new instance of AlertDialogFragment. It's better to have a class instantiator, so the
     * caller does not forget to set the essential parameters. Both parameters shall be non-empty.
     * 
     * @param title dialog title
     * @param message dialog message
     * @return reference to new AlertDialogFragment instance
     */
    public static AlertDialogFragment newInstance(String title, String message) {
        AlertDialogFragment newinstance = new AlertDialogFragment();

        // add dialog title and message arguments to new instance of AlertDialogFragment
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", message);
        newinstance.setArguments(args);

        return newinstance;
    }

    /*
     * 
     */
    /**
     * The listener interface for receiving onDialogPositiveClick event.
     */
    public interface AlertDialogListener {

        /**
         * On click on the dialog OK button.
         * 
         * @param dialog dialog instance
         */
        public void onDialogPositiveClick(DialogFragment dialog);
    }

    /**
     * Override the Fragment.onAttach() method to instantiate the {@link AlertDialogListener}
     * interface.
     * 
     * @see android.support.v4.app.DialogFragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the AlertDialogListener so we can send events to the host
            mListener = (AlertDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement AlertDialogListener");
        }
    }

}
