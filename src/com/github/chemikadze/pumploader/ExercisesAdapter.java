package com.github.chemikadze.pumploader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.TextView;
import com.github.chemikadze.pumploader.model.Exercise;
import com.github.chemikadze.pumploader.model.ExerciseSet;

import java.util.ArrayList;

import static com.github.chemikadze.pumploader.Utils.formatElapsed;

class ExercisesAdapter extends BaseExpandableListAdapter {

    private final Context context;
    private final LayoutInflater inflater;

    private final int groupResource;
    private final int childResource;

    private ArrayList<Exercise> exercises;

    private Utils.Function1<Integer, View.OnClickListener> onAddClickListenerFactory;

    public ExercisesAdapter(Context context, ArrayList<Exercise> groupData, int groupLayout, int childLayout) {
        super();
        this.context = context;
        this.inflater = (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.exercises = groupData;
        this.groupResource = groupLayout;
        this.childResource = childLayout;
    }

    public ArrayList<Exercise> getExercises() {
        return exercises;
    }

    public void removeExercise(int id) {
        exercises.remove(id);
        notifyDataSetChanged();
    }

    public void addExercise(String description) {
        exercises.add(newExercise(description));
        notifyDataSetChanged();
    }

    public void removeSet(int exerciseId, int setId) {
        exercises.get(exerciseId).getSets().remove(setId);
        notifyDataSetChanged();
    }

    public void addSet(int exerciseId, int count, int elapsed) {
        exercises.get(exerciseId).getSets().add(newSet(count, elapsed));
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
        title.setText(exercises.get(groupPosition).getName());

        Button addButton = (Button)view.findViewById(R.id.add_set);
        addButton.setOnClickListener(onAddClickListenerFactory.apply(groupPosition));

        TextView commentView = (TextView)view.findViewById(R.id.exercise_comment);
        if (!exercises.get(groupPosition).getSets().isEmpty()) {
            ArrayList<ExerciseSet> sets = exercises.get(groupPosition).getSets();
            int totalCount = Utils.totalCount(sets);
            int totalDuration = Utils.totalElapsed(sets);

            StringBuilder comment = new StringBuilder();
            String template = context.getResources().getString(R.string.exercise_count_total_fmt);
            comment.append(String.format(template, totalCount));
            if (totalDuration > 0) {
                String elapsedTemplate = context.getResources().getString(R.string.exercise_elapsed_fmt);
                comment.append(", ");
                comment.append(String.format(elapsedTemplate, formatElapsed(totalDuration)));
            }
            commentView.setText(comment);
            commentView.setVisibility(View.VISIBLE);
        } else {
            commentView.setVisibility(View.GONE);
        }

        return view;
    }

    private Exercise newExercise(String name) {
        return new Exercise(name, new ArrayList<ExerciseSet>());
    }

    private ExerciseSet newSet(Integer count, Integer elapsed) {
        return new ExerciseSet(count, elapsed);
    }

    @Override
    public int getGroupCount() {
        return exercises.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return exercises.get(groupPosition).getSets().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return exercises.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return exercises.get(groupPosition).getSets().get(childPosition);
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
        title.setText(String.valueOf(exercises.get(groupPosition).getSets().get(childPosition).getCount()));

        TextView time = (TextView)view.findViewById(R.id.exercise_duration);
        int duration = exercises.get(groupPosition).getSets().get(childPosition).getDuration();
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
