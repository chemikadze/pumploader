package com.github.chemikadze.pumploader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.TextView;
import com.github.chemikadze.pumploader.model.ExerciseSet;
import com.github.chemikadze.pumploader.model.ExerciseReps;

import java.util.ArrayList;

import static com.github.chemikadze.pumploader.Utils.formatElapsed;

class ExercisesAdapter extends BaseExpandableListAdapter {

    private final Context context;
    private final LayoutInflater inflater;

    private final int groupResource;
    private final int childResource;

    private ArrayList<ExerciseSet> exerciseSets;

    private Utils.Function1<Integer, View.OnClickListener> onAddClickListenerFactory;

    public ExercisesAdapter(Context context, ArrayList<ExerciseSet> groupData, int groupLayout, int childLayout) {
        super();
        this.context = context;
        this.inflater = (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.exerciseSets = groupData;
        this.groupResource = groupLayout;
        this.childResource = childLayout;
    }

    public ArrayList<ExerciseSet> getExerciseSets() {
        return exerciseSets;
    }

    public void removeExercise(int id) {
        exerciseSets.remove(id);
        notifyDataSetChanged();
    }

    public void addExercise(String description) {
        exerciseSets.add(newExercise(description));
        notifyDataSetChanged();
    }

    public void removeSet(int exerciseId, int setId) {
        exerciseSets.get(exerciseId).getReps().remove(setId);
        notifyDataSetChanged();
    }

    public void addSet(int exerciseId, int count, int elapsed, int weight) {
        exerciseSets.get(exerciseId).getReps().add(newSet(count, elapsed, weight));
        notifyDataSetChanged();
    }

    public void setOnAddClickListenerFactory(Utils.Function1<Integer, View.OnClickListener> onAddClickListenerFactory) {
        this.onAddClickListenerFactory = onAddClickListenerFactory;
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = inflater.inflate(groupResource, null);
        } else {
            view = convertView;
        }

        TextView title = (TextView)view.findViewById(R.id.exercise_name);
        title.setText(exerciseSets.get(groupPosition).getName());

        Button addButton = (Button)view.findViewById(R.id.add_set);
        addButton.setOnClickListener(onAddClickListenerFactory.apply(groupPosition));

        TextView commentView = (TextView)view.findViewById(R.id.exercise_comment);
        if (!exerciseSets.get(groupPosition).getReps().isEmpty()) {
            ArrayList<ExerciseReps> sets = exerciseSets.get(groupPosition).getReps();
            int totalCount = Utils.totalCount(sets);
            int totalDuration = Utils.totalElapsed(sets);

            StringBuilder comment = new StringBuilder();
            comment.append(context.getResources().getQuantityString(R.plurals.exercise_count_total_fmt, totalCount, totalCount));
            if (totalDuration > 0) {
                String elapsedTemplate = context.getResources().getString(R.string.exercise_elapsed_fmt);
                comment.append(String.format(elapsedTemplate, formatElapsed(totalDuration)));
            }
            commentView.setText(comment);
            commentView.setVisibility(View.VISIBLE);
        } else {
            commentView.setVisibility(View.GONE);
        }

        return view;
    }

    private ExerciseSet newExercise(String name) {
        return new ExerciseSet(name, new ArrayList<ExerciseReps>());
    }

    private ExerciseReps newSet(Integer count, Integer elapsed, Integer weight) {
        return new ExerciseReps(count, elapsed, weight);
    }

    @Override
    public int getGroupCount() {
        return exerciseSets.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return exerciseSets.get(groupPosition).getReps().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return exerciseSets.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return exerciseSets.get(groupPosition).getReps().get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = inflater.inflate(childResource, null);
        } else {
            view = convertView;
        }

        TextView title = (TextView)view.findViewById(R.id.exercise_name);
        ExerciseReps reps = exerciseSets.get(groupPosition).getReps().get(childPosition);
        int count = reps.getCount();
        int weight = reps.getWeight();
        StringBuilder formatted = new StringBuilder();
        formatted.append(count);
        if (weight > 0) {
            formatted.append(" × ");
            formatted.append(context.getResources().getQuantityString(R.plurals.weight_picker_format, weight, weight));
        }
        title.setText(formatted.toString());

        TextView time = (TextView)view.findViewById(R.id.exercise_duration);
        int duration = exerciseSets.get(groupPosition).getReps().get(childPosition).getDuration();
        if (duration > 0) {
            time.setText(formatElapsed(duration));
        } else {
            time.setText("");
        }

        return view;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

}
