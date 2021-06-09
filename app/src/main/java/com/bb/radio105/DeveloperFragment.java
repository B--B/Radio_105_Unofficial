package com.bb.radio105;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

public class DeveloperFragment extends Fragment {

    private View root;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_developer, container, false);

        // Stock Colors
        MainActivity.updateColorsInterface.onUpdate(false);

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                 Navigation.findNavController(root).navigate(R.id.nav_home);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

        TextView sources = root.findViewById(R.id.sources);
        sources.setOnClickListener(view1 -> openWebPage("https://github.com/B--B/Radio_105_Unofficial"));

        TextView bug = root.findViewById(R.id.bug);
        bug.setOnClickListener(view2 -> openWebPage("https://github.com/B--B/Radio_105_Unofficial/issues"));

        TextView developerMail = root.findViewById(R.id.developer_mail);
        developerMail.setOnClickListener(view3 -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", "mrczn.bb@gmail.com", null))
                    .putExtra(Intent.EXTRA_EMAIL, "mrczn.bb@gmail.com");
            startActivity(Intent.createChooser(emailIntent, view3.getContext().getString(R.string.send_email)));
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        root = null;
        super.onDestroyView();
    }

    void openWebPage(String url) {
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        startActivity(intent);
    }
}
