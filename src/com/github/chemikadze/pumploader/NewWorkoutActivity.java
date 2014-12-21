package com.github.chemikadze.pumploader;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.jstrava.connector.JStrava;
import org.jstrava.connector.JStravaV3;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.*;

public class NewWorkoutActivity extends Activity {

    private static final Integer ADD_ACCOUNT_REQUEST = 1;

    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_EXERCISES = "exercises";

    private final String tag = this.getClass().getSimpleName();

    ArrayList<String> exercises;
    ArrayAdapter<String> exerciseAdapter;
    EditText descriptionWidget;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_workout_activity);

        TabHost tabHost = (TabHost)findViewById(R.id.tab_host_input_type);
        tabHost.setup();

        TabHost.TabSpec spec;

        spec = tabHost.newTabSpec(TAG_EXERCISES);
        spec.setContent(R.id.tab_ex);
        spec.setIndicator(getString(R.string.tab_exersises));
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec(TAG_DESCRIPTION);
        spec.setContent(R.id.tab_descr);
        spec.setIndicator(getString(R.string.tab_description));
        tabHost.addTab(spec);

        ListView exerciseListView = (ListView)findViewById(R.id.exersizes);
        LayoutInflater inflater = getLayoutInflater();
        exerciseListView.addFooterView(inflater.inflate(R.layout.add_exercise, null));
        exercises = new ArrayList<String>(0);
        exerciseAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.exercise_item, exercises);
        exerciseListView.setAdapter(exerciseAdapter);
        registerForContextMenu(exerciseListView);

        EditText titleWidget = (EditText)findViewById(R.id.title_text);
        String prefix = getResources().getString(R.string.new_workout_prefix);
        String dateStr = DateFormat.getDateTimeInstance().format(new Date());
        String caption = prefix + " " + dateStr;
        titleWidget.setText(caption);

        if (getIntent().hasExtra(Intent.EXTRA_TEXT)) {
            descriptionWidget = (EditText)findViewById(R.id.description_text);
            String description = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            tabHost.setCurrentTabByTag(TAG_DESCRIPTION);
            descriptionWidget.setText(description);
        } else {
            tabHost.setCurrentTabByTag(TAG_EXERCISES);
        }

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.exercise_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.remove_exercise:
                String exercise = exerciseAdapter.getItem(info.position);
                exerciseAdapter.remove(exercise);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public void onAddExercise(View view) {
        final EditText txtUrl = new EditText(this);

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_exercise))
            .setView(txtUrl)
            .setPositiveButton(getString(R.string.btn_add), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String exercise = txtUrl.getText().toString();
                    doAddExercise(exercise);
                }
            })
            .setNegativeButton(getString(R.string.btn_cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .show();
    }

    private void doAddExercise(String description) {
        exerciseAdapter.add(description);
    }

    public void onUploadClicked(View view) {
        final AccountManager am = AccountManager.get(getApplicationContext());
        Account[] as = am.getAccountsByType(getString(R.string.account_type));
        if (as.length == 0) {
            am.addAccount(getString(R.string.account_type), null, null, null, null, new AccountManagerCallback<Bundle>() {
                @Override
                public void run(AccountManagerFuture<Bundle> future) {
                    try {
                        startActivityForResult((Intent)future.getResult().get(AccountManager.KEY_INTENT), ADD_ACCOUNT_REQUEST);
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Failed to add account", Toast.LENGTH_LONG).show();
                    }
                }
            }, null);
        } else {
            startUpload();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_ACCOUNT_REQUEST) {
            if (resultCode == RESULT_OK) {
                startUpload();
            } else {
                Toast.makeText(getApplicationContext(), "Account was not added, upload cancelled", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String getDescriptionFromConstructor() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < exercises.size(); ++i) {
            text.append(exercises.get(i));
            text.append("\n");
        }
        return text.toString();
    }

    private void startUpload() {
        EditText titleWidget = (EditText)findViewById(R.id.title_text);
        descriptionWidget = (EditText)findViewById(R.id.description_text);
        CheckBox isPrivateWidget = (CheckBox)findViewById(R.id.is_private);

        String title = titleWidget.getText().toString();
        String descriptionText = descriptionWidget.getText().toString().trim();
        String constructorText = getDescriptionFromConstructor().trim();
        StringBuilder description = new StringBuilder();
        description.append(descriptionText);
        if (!descriptionText.isEmpty() && !constructorText.isEmpty()) {
            description.append("\n\n");
        }
        description.append(constructorText);

        uploadWorkout(title, description.toString(), isPrivateWidget.isChecked());

        setResult(RESULT_OK);
        finish();
    }

    private void uploadWorkout(final String title, final String description, final Boolean isPrivate) {
        final AccountManager am = AccountManager.get(getApplicationContext());
        Account[] as = am.getAccountsByType(getString(R.string.account_type));
        if (as.length == 0) {
            Toast.makeText(getApplicationContext(), "Account was not added, upload cancelled", Toast.LENGTH_SHORT).show();
        } else {
            uploadWorkout(as[0], title, description, isPrivate);
        }
    }

    private void uploadWorkout(Account account, final String title, final String description, final Boolean isPrivate) {
        AccountManager am = AccountManager.get(getApplicationContext());
        am.getAuthToken(account, getString(R.string.account_type), null, null, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                String token;
                try {
                    token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                } catch (Exception e) {
                    String msg = "Failed to get account token: " + e.getMessage();
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                    return;
                }
                new UploadActivityTask().execute(token, title, description, isPrivate.toString());
            }
        }, null);
    }

    class UploadActivityTask extends AsyncTask<String, String, Future<Integer>> {
        @Override
        protected Future<Integer> doInBackground(String... params) {
            String token = params[0];
            String title = params[1];
            String description = params[2];
            Boolean isPrivate = Boolean.valueOf(params[3]);

            try {
                JStrava strava = new JStravaV3(token);
                Date date = new Date();
                String dateString = date.toString();
                org.jstrava.entities.activity.Activity a = strava.createActivity(title, "Workout", dateString, 1, description, 0);
                if (isPrivate) {
                    HashMap<String, Object> activityParams = new HashMap<String, Object>();
                    activityParams.put("private", "1");
                    strava.updateActivity(a.getId(), activityParams);
                }
                return new Success<Integer>(a.getId());
            } catch (Exception t) {
                return new Failure<Integer>(new ExecutionException(t.getMessage(), t));
            }
        }

        @Override
        protected void onPostExecute(Future<Integer> r) {
            try {
                Toast.makeText(getApplicationContext(), "Uploaded activity with id " + r.get(), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                String msg = "Failed to upload activity: " + e.getMessage();
                Log.i(tag, msg, e);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        }
    }

    abstract class CompletedFuture<T> implements Future<T> {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return get();
        }
    }

    class Success<T> extends CompletedFuture<T> {
        T value;

        Success(T value) { this.value = value; }

        @Override
        public T get() throws InterruptedException, ExecutionException { return this.value; }
    }

    class Failure<T> extends CompletedFuture<T> {
        ExecutionException e;
        Failure(ExecutionException e) { this.e = e; }
        @Override
        public T get() throws InterruptedException, ExecutionException { throw e; }
    }
}