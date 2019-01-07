package com.borisruzanov.russianwives.mvp.ui.slider;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.arellomobile.mvp.MvpAppCompatFragment;
import com.borisruzanov.russianwives.R;
import com.borisruzanov.russianwives.mvp.model.repository.rating.RatingRepository;
import com.borisruzanov.russianwives.mvp.model.repository.slider.SliderRepository;
import com.borisruzanov.russianwives.utils.Consts;
import com.borisruzanov.russianwives.utils.ValueCallback;

import java.util.HashMap;
import java.util.Map;

import static com.borisruzanov.russianwives.mvp.model.repository.rating.Rating.ADD_AGE_RATING;


public class SliderAgeFragment extends MvpAppCompatFragment {

    //    SliderFragmentsPresenter sliderFragmentsPresenter;
    RadioGroup radioGroup;
    Button btnSave;
    RadioButton radioButton;

    String result;

    public static SliderAgeFragment newInstance() {
        SliderAgeFragment fragment = new SliderAgeFragment();
        Bundle args = new Bundle();
        args.putString(Consts.NEED_BACK, Consts.BACK);
        fragment.setArguments(args);
        return fragment;
    }

    public SliderAgeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_slider_age, container, false);

        btnSave = view.findViewById(R.id.fragment_slider_age_btn_save);
        radioGroup = (RadioGroup) view.findViewById(R.id.fragment_slider_age_radiogroup);
        Log.d("qwe", "onCreateView " );

        new SliderRepository().getFieldFromCurrentUser("age", value -> {
            result = value;
            Log.d("qwe", "value " + result);
            if (value != null && value.equals(getString(R.string.age_18_21))){
                radioGroup.check(R.id.fragment_slider_age_18_21);
            } else if (value != null && value.equals(getString(R.string.age_22_26))){
                radioGroup.check(R.id.fragment_slider_age_22_26);
            } else if (value != null && value.equals(getString(R.string.age_26_35))){
                radioGroup.check(R.id.fragment_slider_age_26_35);
            }else if (value != null && value.equals(getString(R.string.age_36_45))){
                radioGroup.check(R.id.fragment_slider_age_36_45);
            }else if (value != null && value.equals(getString(R.string.age_45_plus))){
                radioGroup.check(R.id.fragment_slider_age_45_plus);
            }
        });

        btnSave.setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            radioButton = view.findViewById(selectedId);
            if (radioButton.getText() != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("age", radioButton.getText());
                new SliderRepository().updateFieldFromCurrentUser(map, () -> {
                    if (result.equals(Consts.DEFAULT)) new RatingRepository().addRating(ADD_AGE_RATING);
                    if (getArguments() != null && getArguments().getString(Consts.NEED_BACK) != null) {
                        getActivity().onBackPressed();
                    }
                    Toast.makeText(getActivity(), R.string.age_updated, Toast.LENGTH_LONG).show();
                });
            }

        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }
}