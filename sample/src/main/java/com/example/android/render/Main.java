/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.render;

import com.android.ide.common.rendering.api.RenderSession;
import com.android.ide.common.rendering.api.Result;
import com.android.ide.common.rendering.api.ViewInfo;
import com.android.ide.common.resources.ResourceItem;
import com.android.ide.common.resources.ResourceRepository;
import com.android.ide.common.resources.ResourceResolver;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.io.FolderWrapper;
import com.android.resources.Density;
import com.android.resources.Keyboard;
import com.android.resources.KeyboardState;
import com.android.resources.Navigation;
import com.android.resources.NavigationState;
import com.android.resources.ScreenOrientation;
import com.android.resources.ScreenRatio;
import com.android.resources.ScreenSize;
import com.android.resources.TouchScreen;
import org.xmlpull.v1.XmlPullParserException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Sample code showing how to use the different API used to achieve a layout rendering.
 * This requires the following jar: layoutlib-api.jar, common.jar, sdk-common.jar, sdklib.jar (although
 * we should get rid of this one) and a full SDK (or at least the platform component).
 *
 */
public class Main {

    // path to the SDK and the project to render
    private final static String SDK = System.getenv().getOrDefault("ANDROID_HOME", System.getProperty("user.home") + File.separator + "Library" + File.separator + "Android" + File.separator + "sdk");

    private final static String CWD = System.getProperty("user.dir");

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (null == SDK || !new File(SDK).exists()) {
            System.out.println("env.ANDROID_HOME not set");
            System.exit(-1);
        }

        String project = CWD + File.separator + "testproject";
        // load the factory for a given platform
        File f = new File(SDK, "platforms" + File.separator + "android-23");

        System.out.println("SDK:      " + SDK);
        System.out.println("Project:  " + project);
        System.out.println("Platform: " + f.getName());

        RenderServiceFactory factory = RenderServiceFactory.create(f);
        if (factory == null) {
            System.err.println("Failed to load platform rendering library");
            System.exit(1);
        }

        // load the project resources
        ResourceRepository projectRes = new ResourceRepository(new FolderWrapper(project + File.separator + "res"), false /*isFramework*/) {
            @Override
            protected ResourceItem createResourceItem(String name) {
                return new ResourceItem(name);
            }
        };
        projectRes.loadResources();

        // create the rendering config
        FolderConfiguration config = RenderServiceFactory.createConfig(
                1280, 800, // size 1 and 2. order doesn't matter.
                           // Orientation will drive which is w and h
                ScreenSize.XLARGE,
                ScreenRatio.LONG,
                ScreenOrientation.LANDSCAPE,
                Density.MEDIUM,
                TouchScreen.FINGER,
                KeyboardState.SOFT,
                Keyboard.QWERTY,
                NavigationState.EXPOSED,
                Navigation.NONAV,
                12); // api level

        // create the resource resolver once for the given config.
        ResourceResolver resources = factory.createResourceResolver(
                config, projectRes,
                "Theme", false /*isProjectTheme*/);

        // create the render service
        RenderService renderService = factory.createService(
                resources, config, new ProjectCallback());

        try {
            RenderSession session = renderService
                    .setLog(new StdOutLogger())
                    .setAppInfo("Layout Library Sample", "icon") // optional
                    .createRenderSession("main" /*layoutName*/);

            // get the status of the render
            Result result = session.getResult();
            if (!result.isSuccess()) {
                System.err.println(result.getErrorMessage());
                Throwable e = result.getException();
                if (e != null) {
                    e.printStackTrace();
                }
                 System.exit(1);
            }

            // get the image and save it somewhere.
            BufferedImage image = session.getImage();
            File png = File.createTempFile("tmp-", ".png");
            ImageIO.write(image, "png", png);
            System.out.println("Rendering result: " + png);

            // read the views
            displayViewObjects(session.getRootViews());

            return;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(1);
    }

    private static void displayViewObjects(List<ViewInfo> rootViews) {
        for (ViewInfo info : rootViews) {
            displayView(info, "");
        }
    }

    private static void displayView(ViewInfo info, String indent) {
        // display info data
        System.out.println(indent + info.getClassName() +
                " [" + info.getLeft() + ", " + info.getTop() + ", " +
                info.getRight() + ", " + info.getBottom() + "]");

        // display the children
        List<ViewInfo> children = info.getChildren();
        if (children != null) {
            indent += "\t";
            for (ViewInfo child : children) {
                displayView(child, indent);
            }
        }
    }
}
