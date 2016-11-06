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

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Define a {@link DialogFragment} to display the error dialog set by
 * {@link ErrorDialogFragment#setDialog(Dialog)}.
 */
public class ErrorDialogFragment extends DialogFragment {

    /** Global field to contain the error dialog. */
    private Dialog mDialog;

    /**
     * Default constructor. Sets the dialog field to null.
     */
    public ErrorDialogFragment() {
        super();
        mDialog = null;
    }

    /**
     * Set the {@link Dialog} to display inside {@link ErrorDialogFragment}.
     * 
     * @param dialog An error dialog
     */
    public void setDialog(Dialog dialog) {
        mDialog = dialog;
    }

    /**
     * This method must return a Dialog to the DialogFragment.
     * 
     * @param savedInstanceState an error dialog
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return mDialog;
    }
}
