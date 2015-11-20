/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.view;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.envirocar.app.R;
import org.envirocar.core.util.Util;

/**
 * Help page
 *
 * @author jakob
 */
public class HelpFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        return inflater.inflate(R.layout.help, container, false);

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //TODO: this could possibly be used to make links in the text able to be opened in the browser
        //gave me an error at first try, though...
        //t2.setMovementMethod(LinkMovementMethod.getInstance());

//		SpannableString text = new SpannableString(getActivity().getText(R.string.help_view_track_text_1));
//		
//		Locale locale = Locale.getDefault();
//		
//		//add symbol in text (position differs depending on language)
//		//TODO you will probably also have to change the data_privacy image here (text on the image is in german right now) 
//		if(locale.equals(Locale.GERMANY)){
//			text.setSpan(is, 103, 117, 0);
//		}else if(locale.equals(Locale.UK) || locale.equals(Locale.US)){
//			text.setSpan(is, 103, 117, 0);
//			
//		}


        TextView versionTextview = (TextView) getActivity().findViewById(R.id.textView22);

        CharSequence versionString = getActivity().getText(R.string.help_text_6_3);

        versionString = versionString + " " + Util.getVersionString(getActivity());

        versionTextview.setText(versionString);

    }
}
