package com.apk.axmleditor.Utils;

import androidx.lifecycle.ViewModel;

import com.apk.axmleditor.serializable.FilesEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2025
 */
public class FilesViewModel extends ViewModel {

    private final Map<String, List<FilesEntry>> map = new HashMap<>();

    public void setFiles(String key, List<FilesEntry> files) {
        map.put(key, files);
    }

    public List<FilesEntry> getFiles(String key) {
        return map.get(key);
    }

}