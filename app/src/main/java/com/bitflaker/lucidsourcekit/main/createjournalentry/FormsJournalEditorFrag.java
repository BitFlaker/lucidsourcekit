package com.bitflaker.lucidsourcekit.main.createjournalentry;

import android.app.Fragment;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.flexbox.FlexboxLayout;

public class FormsJournalEditorFrag extends Fragment {
    public static FormsJournalEditorFrag newInstance() {
        return new FormsJournalEditorFrag();
    }

    private FlexboxLayout formsContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dream_forms_story, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        formsContainer = getView().findViewById(R.id.flx_form_dream);

        String formsTemplate = "I was in <<EDIT>> and I saw <<EDIT>>. The daytime was <<EDIT>>. Characters in my dream were <<EDIT>>. I was <<EDIT>>. The characters in my dream behaved <<EDIT>>."; // TODO make this text editable for users
        String[] formsTemplateSplit = formsTemplate.split("<<EDIT>>");
        for (int i = 0; i < formsTemplateSplit.length; i++) {
            if (startsWithSentenceEnd(formsTemplateSplit[i])) {
                String[] separatedSentences = separateAtSentenceEnd(formsTemplateSplit[i]);
                formsContainer.addView(generateTextView(separatedSentences[0]));
                TextView tv = generateTextView(separatedSentences[1]);
                if(tv != null){
                    formsContainer.addView(tv);
                }
            }
            else{
                TextView tv = generateTextView(formsTemplateSplit[i]);
                if(tv != null){
                    formsContainer.addView(tv);
                }
            }
            if(i < formsTemplateSplit.length - 1) {
                formsContainer.addView(generateEditText());
            }
        }
    }

    private EditText generateEditText() {
        EditText et = new EditText(getContext());
        et.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        et.setMinWidth(Tools.dpToPx(getContext(), 70));
        return et;
    }

    private TextView generateTextView(String sentence) {
        if(sentence.length() == 0){
            return null;
        }
        TextView tv = new TextView(getContext());
        tv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tv.setText(sentence);
        return tv;
    }

    private String[] separateAtSentenceEnd(String sentence) {
        String[] separated = new String[2];
        int splitPos = getLastSentenceEnd(sentence);
        if(splitPos < sentence.length()){
            separated[0] = sentence.substring(0, splitPos + 1);
            separated[1] = sentence.substring(splitPos + 1);
        }
        else {
            separated[0] = sentence.substring(0, splitPos);
            separated[1] = "";
        }
        return separated;
    }

    private int getLastSentenceEnd(String sentence) {
        for (int i = 0; i < sentence.length(); i++){
            if(!isSentenceEndSymbol(sentence.charAt(i))) {
                return i;
            }
        }
        return sentence.length();
    }

    private boolean isSentenceEndSymbol(char c) {
        char[] sentenceEnds = new char[]{'.', '!', '?'};
        for (char sentenceEnd : sentenceEnds) {
            if (sentenceEnd == c) { return true; }
        }
        return false;
    }

    private boolean startsWithSentenceEnd(String sentence) {
        if(sentence.length() == 0){
            return false;
        }
        return isSentenceEndSymbol(sentence.charAt(0));
    }

    public String getFormResult() {
        int count = formsContainer.getChildCount();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++){
            View v = formsContainer.getChildAt(i);
            if(v instanceof EditText){
                sb.append(((TextView) v).getText());
            }
            else if (v instanceof TextView) {
                sb.append(((TextView) v).getText());
            }
        }
        return sb.toString();
    }
}
