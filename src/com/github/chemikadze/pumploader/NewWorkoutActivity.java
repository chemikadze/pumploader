package com.github.chemikadze.pumploader;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import org.jstrava.connector.JStrava;
import org.jstrava.connector.JStravaV3;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.*;

public class NewWorkoutActivity extends Activity {

    private static final Integer ADD_ACCOUNT_REQUEST = 1;

    private String tag = this.getClass().getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_workout_activity);
        EditText titleWidget = (EditText)findViewById(R.id.title_text);
        EditText descriptionWidget = (EditText)findViewById(R.id.description_text);

        String prefix = getResources().getString(R.string.new_workout_prefix);
        String dateStr = DateFormat.getDateTimeInstance().format(new Date());
        String caption = prefix + " " + dateStr;
        String description = getIntent().getStringExtra(Intent.EXTRA_TEXT);

        titleWidget.setText(caption);
        descriptionWidget.setText(description);
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

    private void startUpload() {
        EditText titleWidget = (EditText)findViewById(R.id.title_text);
        EditText descriptionWidget = (EditText)findViewById(R.id.description_text);
        CheckBox isPrivateWidget = (CheckBox)findViewById(R.id.is_private);

        String title = titleWidget.getText().toString(), description = descriptionWidget.getText().toString();
        uploadWorkout(title, description, isPrivateWidget.isChecked());

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
                    String msg = "Failed to get token: " + e.getMessage();
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                    return;
                }
                String msg = "Uploading workout with token " + token + ":\n\n" + title + "\n\n" + description;
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
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
                String msg = "Failed to upload workout: " + e.getMessage();
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