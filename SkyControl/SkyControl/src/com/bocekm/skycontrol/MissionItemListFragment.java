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

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bocekm.skycontrol.mission.Mission;
import com.bocekm.skycontrol.mission.MissionEvents.MissionEvent;
import com.bocekm.skycontrol.mission.MissionEvents.MissionListener;
import com.bocekm.skycontrol.mission.item.NavMissionItem;

/**
 * Map sidebar fragment displaying list of mission waypoints.
 */
public class MissionItemListFragment extends ListFragment implements
        MissionListener {

    /** List of waypoints. */
    private List<NavMissionItem> mWaypointList;

    /** The adapter for the ListView displaying the mission items. */
    private MissionItemAdapter mAdapter;

    /** Constant used to retreive value sent back from {@link NumberPickerDialog}. */
    private static final int ALTITUDE_REQUEST = 0;

    /** Reference to long-clicked waypoint so we can update it when the user changes its altitude. */
    private NavMissionItem mClickedWaypoint = null;

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get list of waypoints and set this list to the adapter
        mWaypointList = Mission.get().getNavMissionItems();
        mAdapter = new MissionItemAdapter(mWaypointList);
        setListAdapter(mAdapter);

        Mission.get().getEvents().addMissionListener(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.ListFragment#onViewCreated(android.view.View, android.os.Bundle)
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Once the view is created highlight selected waypoint
        ((LocationFragment) getParentFragment()).renderWaypoints();

        setEmptyText("No waypoints");
        // Change the default transparent color of view background to Holo theme screen background
        // color
        view.setBackgroundColor(Color.parseColor("#fff3f3f3"));
        // Allow to activate one list item at a time
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        // Allow user to change the altitude of waypoint by long-click
        getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
            /*
             * (non-Javadoc)
             * 
             * @see
             * android.widget.AdapterView.OnItemLongClickListener#onItemLongClick(android.widget
             * .AdapterView, android.view.View, int, long)
             */
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // Get reference to clicked waypoint
                mClickedWaypoint = Mission.get().getNavMissionItems().get(position);
                // Highlight marker corresponding to the clicked waypoint
                ((LocationFragment) getParentFragment()).highlightWaypoint(mClickedWaypoint, false);

                // Show altitude change dialog
                FragmentManager fm = getActivity().getSupportFragmentManager();
                NumberPickerDialog dialog =
                        NumberPickerDialog.newInstance(
                                mClickedWaypoint.getAltitude(),
                                0,
                                getResources().getInteger(R.integer.max_waypoint_altitude),
                                getString(R.string.altitude_dialog_title, getResources()
                                        .getInteger(R.integer.max_waypoint_altitude)));
                dialog.setTargetFragment(MissionItemListFragment.this, ALTITUDE_REQUEST);
                dialog.show(fm, null);

                return false;
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.ListFragment#onListItemClick(android.widget.ListView,
     * android.view.View, int, long)
     */
    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        // Highlight marker corresponding to the clicked waypoint
        ((LocationFragment) getParentFragment()).highlightWaypoint(Mission.get()
                .getNavMissionItems().get(position), true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;
        switch (requestCode) {
            case ALTITUDE_REQUEST:
                // Retrieve the new waypoint altitude set by user
                mClickedWaypoint.setAltitude(data.getIntExtra(
                        NumberPickerDialog.EXTRA_PICKED_NUMBER, 0));
                mClickedWaypoint.toastOnCollision();
                mAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    /**
     * {@link MissionItemAdapter} provides the waypoint items for the list on the as-needed basis.
     */
    private class MissionItemAdapter extends ArrayAdapter<NavMissionItem> {
        /**
         * Instantiates a new {@link MissionItemAdapter}.
         * 
         * @param items list containing the waypoints
         */
        public MissionItemAdapter(List<NavMissionItem> items) {
            // Not using pre-defined layout, so we pass 0 for the layout ID
            super(getActivity(), 0, items);
        }

        /*
         * (non-Javadoc) Processes one row of the list and manages how the row looks like.
         * 
         * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // If we weren't given a view, inflate one
            if (convertView == null) {
                convertView =
                        getActivity().getLayoutInflater()
                                .inflate(R.layout.list_item_waypoint, null);
            }
            // Configure the view for this waypoint
            final NavMissionItem item = getItem(position);

            TextView waypointTitleTextView =
                    (TextView) convertView.findViewById(R.id.waypoint_title);
            waypointTitleTextView.setText(item.getTitle());

            TextView waypointAltTextView =
                    (TextView) convertView.findViewById(R.id.waypoint_subtitle);
            waypointAltTextView.setText(item.getSubTitle());

            if (item.doesCollide()) {
                waypointTitleTextView.setTextColor(getResources().getColor(R.color.skycontrol_red));
                waypointAltTextView.setTextColor(getResources().getColor(R.color.skycontrol_red));
            } else {
                waypointTitleTextView.setTextColor(getResources().getColor(android.R.color.black));
                waypointAltTextView.setTextColor(getResources().getColor(android.R.color.black));
            }

            ImageView deleteWaypointButton =
                    (ImageView) convertView.findViewById(R.id.delete_waypoint_button);
            // Set action to be performed on delete waypoint button click
            deleteWaypointButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    // Notify the parent LocationFragment that user deleted a waypoint
                    SkyControlUtils.toast(item.getTitle() + " deleted", Toast.LENGTH_SHORT);
                    ((LocationFragment) getParentFragment()).keepWaypointIndex(item
                            .getIndexInNavMission());
                    // Remove the waypoint from the list of mission items
                    Mission.get().removeMissionItem(item);
                }
            });
            return convertView;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bocekm.skycontrol.mission.MissionEvents.MissionListener#onMissionEvent(com.bocekm.skycontrol
     * .mission.MissionEvents.MissionEvent)
     */
    @Override
    public void onMissionEvent(MissionEvent event) {
        switch (event) {
            case MISSION_UPDATE:
                // Update the content of the fragment as the mission items changed
                mAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onDestroy()
     */
    @Override
    public void onDestroy() {
        Mission.get().getEvents().removeMissionListener(this);
        super.onDestroy();
    }

}
