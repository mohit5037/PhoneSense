package com.probisticktechnologies.PhoneSense;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.probisticktechnologies.R;


public class HomeScreenActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ChooseApplicationFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class ChooseApplicationFragment extends Fragment {

        // ListView Variable
        private ListView chooseApplicationListView;

        // List of possible applications
        private String[] applicationNames = new String[]{
                "Phone Sensor Data Capture"
        };

        public ChooseApplicationFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_choose_application, container, false);

            chooseApplicationListView = (ListView) rootView.findViewById(R.id.choose_application_listView);

            // Creating an array adapter which contains data for listview
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_list_item_1, android.R.id.text1, applicationNames);

            // attaching listview with the adapter
            chooseApplicationListView.setAdapter(adapter);

            // Setting up the listener for list view
            chooseApplicationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    // Getting the name of item clicked
                    String appToBeLaunched = (String) chooseApplicationListView
                            .getItemAtPosition(position);

                    // Launching that activity
                    try {
                        Intent intent = new Intent(getActivity(),
                                Class.forName(getActivity().getPackageName()
                                        + "." + appToBeLaunched.replaceAll("\\s+","")));
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });

            return rootView;
        }
    }
}
