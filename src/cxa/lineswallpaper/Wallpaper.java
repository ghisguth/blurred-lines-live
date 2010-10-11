package cxa.lineswallpaper;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

import net.rbgrn.opengl.GLWallpaperService;
import android.opengl.GLSurfaceView.EGLConfigChooser;
import android.opengl.GLSurfaceView.EGLContextFactory;

public class Wallpaper extends GLWallpaperService {

	class GLES20ContextFactory implements EGLContextFactory {
		private int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

		@Override
		public EGLContext createContext(final EGL10 egl,
				final EGLDisplay display, final EGLConfig config) {
			int[] attrib_list = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
			return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT,
					attrib_list);
		}

		@Override
		public void destroyContext(final EGL10 egl, final EGLDisplay display,
				final EGLContext context) {
			egl.eglDestroyContext(display, context);
		}
	}

	private abstract class BaseConfigChooser implements EGLConfigChooser {
		public BaseConfigChooser(int[] configSpec) {
			mConfigSpec = filterConfigSpec(configSpec);
		}

		public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
			int[] num_config = new int[1];
			if (!egl.eglChooseConfig(display, mConfigSpec, null, 0, num_config)) {
				throw new IllegalArgumentException("eglChooseConfig failed");
			}

			int numConfigs = num_config[0];

			if (numConfigs <= 0) {
				throw new IllegalArgumentException(
						"No configs match configSpec");
			}

			EGLConfig[] configs = new EGLConfig[numConfigs];
			if (!egl.eglChooseConfig(display, mConfigSpec, configs, numConfigs,
					num_config)) {
				throw new IllegalArgumentException("eglChooseConfig#2 failed");
			}
			EGLConfig config = chooseConfig(egl, display, configs);
			if (config == null) {
				throw new IllegalArgumentException("No config chosen");
			}
			return config;
		}

		abstract EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
				EGLConfig[] configs);

		protected int[] mConfigSpec;

		private int[] filterConfigSpec(int[] configSpec) {
			/*if (mEGLContextClientVersion != 2) {
				return configSpec;
			}*/
			/*
			 * We know none of the subclasses define EGL_RENDERABLE_TYPE. And we
			 * know the configSpec is well formed.
			 */
			int len = configSpec.length;
			int[] newConfigSpec = new int[len + 2];
			System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1);
			newConfigSpec[len - 1] = EGL10.EGL_RENDERABLE_TYPE;
			newConfigSpec[len] = 4; /* EGL_OPENGL_ES2_BIT */
			newConfigSpec[len + 1] = EGL10.EGL_NONE;
			return newConfigSpec;
		}
	}

	/**
	 * Choose a configuration with exactly the specified r,g,b,a sizes, and at
	 * least the specified depth and stencil sizes.
	 */
	private class ComponentSizeChooser extends BaseConfigChooser {
		public ComponentSizeChooser(int redSize, int greenSize, int blueSize,
				int alphaSize, int depthSize, int stencilSize) {
			super(new int[] { EGL10.EGL_RED_SIZE, redSize,
					EGL10.EGL_GREEN_SIZE, greenSize, EGL10.EGL_BLUE_SIZE,
					blueSize, EGL10.EGL_ALPHA_SIZE, alphaSize,
					EGL10.EGL_DEPTH_SIZE, depthSize, EGL10.EGL_STENCIL_SIZE,
					stencilSize, EGL10.EGL_NONE });
			mValue = new int[1];
			mRedSize = redSize;
			mGreenSize = greenSize;
			mBlueSize = blueSize;
			mAlphaSize = alphaSize;
			mDepthSize = depthSize;
			mStencilSize = stencilSize;
		}

		@Override
		public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
				EGLConfig[] configs) {
			for (EGLConfig config : configs) {
				int d = findConfigAttrib(egl, display, config,
						EGL10.EGL_DEPTH_SIZE, 0);
				int s = findConfigAttrib(egl, display, config,
						EGL10.EGL_STENCIL_SIZE, 0);
				if ((d >= mDepthSize) && (s >= mStencilSize)) {
					int r = findConfigAttrib(egl, display, config,
							EGL10.EGL_RED_SIZE, 0);
					int g = findConfigAttrib(egl, display, config,
							EGL10.EGL_GREEN_SIZE, 0);
					int b = findConfigAttrib(egl, display, config,
							EGL10.EGL_BLUE_SIZE, 0);
					int a = findConfigAttrib(egl, display, config,
							EGL10.EGL_ALPHA_SIZE, 0);
					if ((r == mRedSize) && (g == mGreenSize)
							&& (b == mBlueSize) && (a == mAlphaSize)) {
						return config;
					}
				}
			}
			return null;
		}

		private int findConfigAttrib(EGL10 egl, EGLDisplay display,
				EGLConfig config, int attribute, int defaultValue) {

			if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
				return mValue[0];
			}
			return defaultValue;
		}

		private int[] mValue;
		// Subclasses can adjust these values:
		protected int mRedSize;
		protected int mGreenSize;
		protected int mBlueSize;
		protected int mAlphaSize;
		protected int mDepthSize;
		protected int mStencilSize;
	}

	/**
	 * This class will choose a RGB_565 surface with or without a depth buffer.
	 * 
	 */
	private class SimpleEGLConfigChooser extends ComponentSizeChooser {
		public SimpleEGLConfigChooser(boolean withDepthBuffer) {
			super(5, 6, 5, 0, withDepthBuffer ? 16 : 0, 0);
		}
	}

	class WallpaperEngine extends GLEngine {
		private static final String TAG = "BlurredLinesLiveWallpaperEngine";

		public WallpaperEngine() {
			super();

			setEGLContextFactory(new GLES20ContextFactory());
			setEGLConfigChooser(new SimpleEGLConfigChooser(true));

			GLES20LinesRenderer renderer = new GLES20LinesRenderer(null);
			setRenderer(renderer);
			setRenderMode(RENDERMODE_CONTINUOUSLY);
		}
	}

	@Override
	public Engine onCreateEngine() {
		return new WallpaperEngine();
	}
}
