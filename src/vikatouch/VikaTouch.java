package vikatouch;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.rms.RecordStore;

import org.json.me.JSONException;
import org.json.me.JSONObject;

import ru.nnproject.vikatouch.VikaTouchApp;
import ru.nnproject.vikaui.UIThread;
import ru.nnproject.vikaui.popup.InfoPopup;
import ru.nnproject.vikaui.popup.VikaNotice;
import ru.nnproject.vikaui.popup.VikaNotification;
import ru.nnproject.vikaui.screen.VikaScreen;
import ru.nnproject.vikaui.utils.DisplayUtils;
import ru.nnproject.vikaui.utils.images.IconsManager;
import vikatouch.caching.ImageStorage;
import vikatouch.canvas.VikaCanvasInst;
import vikatouch.locale.TextLocal;
import vikatouch.screens.AboutScreen;
import vikatouch.screens.CaptchaScreen;
import vikatouch.screens.ChatScreen;
import vikatouch.screens.DialogsScreen;
import vikatouch.screens.LoginScreen;
import vikatouch.screens.MainScreen;
import vikatouch.screens.NewsScreen;
import vikatouch.screens.ReturnableListScreen;
import vikatouch.screens.menu.DocsScreen;
import vikatouch.screens.menu.FriendsScreen;
import vikatouch.screens.menu.GroupsScreen;
import vikatouch.screens.menu.MenuScreen;
import vikatouch.screens.menu.PhotosScreen;
import vikatouch.screens.menu.VideosScreen;
import vikatouch.screens.page.GroupPageScreen;
import vikatouch.screens.temp.SplashScreen;
import vikatouch.settings.Settings;
import vikatouch.settings.SettingsScreen;
import vikatouch.utils.ResizeUtils;
import vikatouch.utils.VikaUtils;
import vikatouch.utils.captcha.CaptchaObject;
import vikatouch.utils.emulatordetect.EmulatorDetector;
import vikatouch.utils.error.ErrorCodes;
import vikatouch.utils.text.TextEditor;
import vikatouch.utils.url.URLBuilder;

public class VikaTouch {

	public static boolean DEMO_MODE = false;
	public static final String API_VERSION = "5.122";
	public static final String TOKEN_RMS = "vikatouchtoken";
	public static final int INDEX_FALSE = -1;
	public static String API = "http://vk-api-proxy.xtrafrancyz.net:80";
	public static String OAUTH = "https://oauth.vk.com:443";
	public static String accessToken;
	public static String mobilePlatform;
	public static LoginScreen loginScr;
	public static MenuScreen menuScr;
	public static DialogsScreen dialogsScr;
	public static NewsScreen newsScr;
	public static CaptchaScreen captchaScr;
	public static RecordStore tokenRMS;
	public static Image cameraImg;
	// public static Image camera48Img;
	public static Thread mainThread;
	public static UIThread uiThread;
	public static String userId;
	public static short unreadCount = -1;
	public static boolean offlineMode;
	public static boolean loading;
	public static AboutScreen about;
	public static VikaCanvasInst canvas;
	public CommandsImpl cmdsInst;
	private String errReason;
	private String tokenAnswer;
	private SplashScreen splash;
	public static VikaTouch inst;
	public static VikaTouchApp appInst;
	public static boolean crashed;
	public static SettingsScreen setsScr;
	public static boolean isEmulator;
	public static boolean musicIsProxied;

	private void saveToken() {
		try {
			try {
				if (tokenRMS != null)
					tokenRMS.closeRecordStore();
				RecordStore.deleteRecordStore(TOKEN_RMS);
			} catch (Exception e) {

			}
			tokenRMS = RecordStore.openRecordStore(TOKEN_RMS, true);
			String s = accessToken + ";" + userId + ";" + MenuScreen.name + " " + MenuScreen.lastname + ";"
					+ MenuScreen.avaurl;
			tokenRMS.addRecord(s.getBytes("UTF-8"), 0, s.length());
			tokenRMS.closeRecordStore();
			// VikaTouch.sendLog("savetoken: "+accessToken);

		} catch (Exception e) {
			VikaTouch.error(e, ErrorCodes.TOKENSAVE);
		}
	}

	private boolean getToken() {
		try {
			tokenRMS = RecordStore.openRecordStore(TOKEN_RMS, true);
			if (tokenRMS.getNumRecords() > 0) {
				String s = new String(tokenRMS.getRecord(1), "UTF-8");
				accessToken = s.substring(0, s.indexOf(";"));

				// Вся эта хрень нужна для запуска в оффлайне
				String s2 = s.substring(s.indexOf(";") + 1, s.length());
				String s3 = s2.substring(s2.indexOf(";") + 1, s2.length());
				MenuScreen.avaurl = s3.substring(s3.indexOf(";") + 1, s3.length());
				MenuScreen.hasAva = true;
				String name = s3.substring(0, s3.indexOf(";"));
				MenuScreen.name = name.substring(0, name.indexOf(" "));
				MenuScreen.lastname = name.substring(name.indexOf(" ") + 1, name.length());
				userId = s2.substring(0, s2.indexOf(";"));
				tokenRMS.closeRecordStore();
				// VikaTouch.sendLog("gettoken: "+accessToken);
				// оптимизация
				MenuScreen.avaurl = null;
				return true;
			}
			tokenRMS.closeRecordStore();
		} catch (Exception e) {
			VikaTouch.error(e, ErrorCodes.TOKENLOAD);
		}
		return false;
	}

	public static void setDisplay(VikaScreen s, int direction) {
		if (s == null) {
			if (accessToken == null || accessToken.length() < 2) {
				s = loginScr;
			} else {
				s = menuScr;
			}
		}
		if (!Settings.dontBack || Settings.animateTransition) {
			if (direction != -1 && s instanceof MainScreen && canvas.currentScreen instanceof MainScreen)
				((MainScreen) s).backScreen = (MainScreen) canvas.currentScreen;
			//if (!Settings.animateTransition)
			//	canvas.oldScreen = canvas.currentScreen;
		}
		appInst.isPaused = false;
		if (s instanceof MenuScreen) {
			DisplayUtils.current = DisplayUtils.CANVAS_MENU;
			MainScreen.lastMenu = DisplayUtils.CANVAS_MENU;
		}
		if (s instanceof NewsScreen) {
			DisplayUtils.current = DisplayUtils.CANVAS_NEWS;
		}
		if (s instanceof DialogsScreen) {
			DisplayUtils.current = DisplayUtils.CANVAS_CHATSLIST;
		}
		if (s instanceof AboutScreen) {
			DisplayUtils.current = DisplayUtils.CANVAS_ABOUT;
		}
		if (s instanceof LoginScreen) {
			DisplayUtils.current = DisplayUtils.CANVAS_LOGIN;
		}
		if (s instanceof ChatScreen) {
			DisplayUtils.current = DisplayUtils.CANVAS_CHAT;
		}
		if (s instanceof ReturnableListScreen) {
			DisplayUtils.current = DisplayUtils.CANVAS_TEMPLIST;
		}
		if (s instanceof GroupPageScreen) {
			DisplayUtils.current = DisplayUtils.CANVAS_TEMPLIST;
			canvas.lastTempScreen = s;
		}
		if (s instanceof DocsScreen) {
			DisplayUtils.current = DisplayUtils.CANVAS_DOCSLIST;
			MainScreen.lastMenu = DisplayUtils.CANVAS_DOCSLIST;
		}
		if (s instanceof GroupsScreen) {
			DisplayUtils.current = DisplayUtils.CANVAS_GROUPSLIST;
			MainScreen.lastMenu = DisplayUtils.CANVAS_GROUPSLIST;
		}
		if (s instanceof FriendsScreen) {
			DisplayUtils.current = DisplayUtils.CANVAS_FRIENDSLIST;
			MainScreen.lastMenu = DisplayUtils.CANVAS_FRIENDSLIST;
		}
		if (s instanceof PhotosScreen) {
			DisplayUtils.current = DisplayUtils.CANVAS_PHOTOSLIST;
			MainScreen.lastMenu = DisplayUtils.CANVAS_PHOTOSLIST;
		}
		if (s instanceof VideosScreen) {
			DisplayUtils.current = DisplayUtils.CANVAS_VIDEOSLIST;
			MainScreen.lastMenu = DisplayUtils.CANVAS_VIDEOSLIST;
		}
		//canvas.slide = direction;
		canvas.currentScreen = s;
		canvas.paint();
		DisplayUtils.checkdisplay();
		// loading = true;
	}

	public static boolean isPaused() {
		return appInst.isPaused;
	}

	public boolean login(final String user, final String pass) {
		// илья ты заебал со своей безопасностью, пошел нахуй
		if (user == null || user.length() == 0) {
			errReason = "login is invalid";
			return false;
		}
		try {
			if (!Settings.proxy) {
				Settings.proxy = false;
				Settings.https = true;
				OAUTH = Settings.httpsOAuth;
				API = Settings.httpsApi;
			} else {
				OAUTH = Settings.proxyOAuth;
				API = Settings.proxyApi;
			}

			tokenAnswer = VikaUtils.download(new URLBuilder(OAUTH, "token").addField("grant_type", "password")
					.addField("client_id", "2685278").addField("client_secret", "lxhD8OD7dMsqtXIm5IUY")
					.addField("username", user).addField("password", pass)
					.addField("scope",
							"notify,friends,photos,audio,video,docs,notes,pages,status,offers,questions,wall,groups,messages,notifications,stats,ads,offline")
					.addField("2fa_supported", 1).addField("force_sms", 1));
			VikaTouch.notificate(
					new VikaNotification(VikaNotification.ERROR, "Auth debug", "1" + tokenAnswer, null));
			if (tokenAnswer == null && !Settings.proxy) {
				VikaTouch.notificate(new VikaNotification(VikaNotification.ERROR, "Direct oauth failed",
						"Connecting via proxy.", null));
				Settings.proxy = true;
				Settings.https = false;
				OAUTH = Settings.proxyOAuth;
				tokenAnswer = VikaUtils.download(new URLBuilder(OAUTH, "token").addField("grant_type", "password")
						.addField("client_id", "2685278").addField("client_secret", "lxhD8OD7dMsqtXIm5IUY")
						.addField("username", user).addField("password", pass)
						.addField("scope",
								"notify,friends,photos,audio,video,docs,notes,pages,status,offers,questions,wall,groups,messages,notifications,stats,ads,offline")
						.addField("2fa_supported", 1).addField("force_sms", 1));
			}
			VikaTouch.notificate(
					new VikaNotification(VikaNotification.ERROR, "Auth debug", "2" + tokenAnswer, null));
			if (tokenAnswer == null) {
				errReason = "Network error!";
				return false;
			}

			errReason = tokenAnswer;
			if (tokenAnswer.indexOf("error") >= 0) {
				if (tokenAnswer.indexOf("need_captcha") > 0) {
					return captcha(user, pass);
				}
				if (tokenAnswer.indexOf("2fa") > 0) {
					return code(user, pass, tokenAnswer);
				}
				errReason = tokenAnswer;
				VikaTouch.notificate(
						new VikaNotification(VikaNotification.ERROR, "Auth debug", "3" + tokenAnswer, null));
				return false;
			} else {
				JSONObject json = new JSONObject(tokenAnswer);
				accessToken = json.getString("access_token");
				userId = json.getString("user_id");
				VikaTouch.notificate(
						new VikaNotification(VikaNotification.ERROR, "Auth debug", "4" + tokenAnswer, null));
				refreshToken();
				VikaTouch.notificate(
						new VikaNotification(VikaNotification.ERROR, "Auth debug", "5" + tokenAnswer, null));
				saveToken();
				VikaTouch.notificate(
						new VikaNotification(VikaNotification.ERROR, "Auth debug", "6" + tokenAnswer, null));
				VikaUtils.download(new URLBuilder("groups.join").addField("group_id", 168202266));
				MenuScreen canvas = menuScr = new MenuScreen();
				VikaTouch.notificate(
						new VikaNotification(VikaNotification.ERROR, "Auth debug", "7" + tokenAnswer, null));
				setDisplay(canvas, 1);

				Dialogs.refreshDialogsList(true, false);
				VikaTouch.notificate(
						new VikaNotification(VikaNotification.ERROR, "Auth debug", "8" + tokenAnswer, null));
				return true;
			}
		} catch (Throwable e) {
			errReason = e.getMessage();
			VikaTouch.notificate(new VikaNotification(VikaNotification.ERROR, "Auth failed", errReason, null));
			// VikaTouch.popup(new InfoPopup(e.toString(), null,
			// TextLocal.inst.get("player.playererror"), null));
			return false;
		}
	}

	private boolean code(String user, String pass, String tokenUnswer) {
		String code = TextEditor.inputString("2Fa code", "", 16);
		try {
			tokenUnswer = VikaUtils.download(new URLBuilder(OAUTH, "token").addField("grant_type", "password")
					.addField("client_id", "2685278").addField("client_secret", "lxhD8OD7dMsqtXIm5IUY")
					.addField("username", user).addField("password", pass)
					.addField("scope",
							"notify,friends,photos,audio,video,docs,notes,pages,status,offers,questions,wall,groups,messages,notifications,stats,ads,offline")
					.addField("2fa_supported", 1).addField("force_sms", 1).addField("code", code).toString());

			if (tokenUnswer == null) {
				errReason = "network error!";
				return false;
			}
			System.out.println(tokenUnswer);
			errReason = tokenUnswer;
			if (tokenUnswer.indexOf("error") >= 0) {
				if (tokenUnswer.indexOf("need_captcha") > 0) {
					return captcha(user, pass);
				}
				if (tokenUnswer.indexOf("2fa") > 0) {
					return code(user, pass, tokenUnswer);
				}
				errReason = tokenUnswer;
				return false;
			} else {
				JSONObject json = new JSONObject(tokenUnswer);
				accessToken = json.getString("access_token");
				userId = json.getString("user_id");
				refreshToken();
				final VikaScreen canvas = menuScr = new MenuScreen();
				setDisplay(canvas, 1);
				saveToken();
				Dialogs.refreshDialogsList(true, false);
				return true;
			}
		} catch (NullPointerException e) {
			errReason = "no internet: " + e.toString();
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			errReason = e.toString();
			e.printStackTrace();
			return false;
		}
	}

	private void refreshToken() {
		String refreshToken;
		String m = VikaUtils.music(URLBuilder.makeSimpleURL("audio.get"));
		if (m.indexOf("confirmation") >= 0) {
			String recept = ":APA91bFAM-gVwLCkCABy5DJPPRH5TNDHW9xcGu_OLhmdUSA8zuUsBiU_DexHrTLLZWtzWHZTT5QUaVkBk_GJVQyCE_yQj9UId3pU3vxvizffCPQISmh2k93Fs7XH1qPbDvezEiMyeuLDXb5ebOVGehtbdk_9u5pwUw";
			String surl = new URLBuilder(API, "auth.refreshToken", false).addField("access_token", accessToken)
					.addField("v", "5.120").addField("receipt", recept).toString();
			refreshToken = VikaUtils.download(surl);
			// VikaTouch.sendLog("refr1 "+refreshToken);
			try {
				if (refreshToken.indexOf("Unknown method") != -1) {
					musicIsProxied = true;
					refreshToken = VikaUtils.music(surl);
					// VikaTouch.sendLog("unk "+refreshToken);
					JSONObject resp = new JSONObject(refreshToken).getJSONObject("response");
					accessToken = resp.getString("token");
				} else {
					JSONObject resp = new JSONObject(refreshToken).getJSONObject("response");
					accessToken = resp.getString("token");
					// VikaTouch.sendLog("refr2 "+accessToken);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			JSONObject resp = new JSONObject(m).getJSONObject("response");
			accessToken = resp.getString("token");
		}
	}

	private boolean captcha(String user, String pass) {
		try {
			captchaScr = new CaptchaScreen();
			captchaScr.obj = new CaptchaObject(new JSONObject(tokenAnswer));
			captchaScr.obj.parseJSON();
			canvas.showCaptcha = true;
			CaptchaScreen.finished = false;
			while (appInst.started) {
				if (captchaScr != null && CaptchaScreen.finished) {
					tokenAnswer = VikaUtils.download(new URLBuilder(OAUTH, "token").addField("grant_type", "password")
							.addField("client_id", "2685278").addField("client_secret", "lxhD8OD7dMsqtXIm5IUY")
							.addField("username", user).addField("password", pass)
							.addField("scope",
									"notify,friends,photos,audio,video,docs,notes,pages,status,offers,questions,wall,groups,messages,notifications,stats,ads,offline")
							.addField("captcha_sid", captchaScr.obj.captchasid)
							.addField("captcha_key", CaptchaScreen.input).toString());
					errReason = tokenAnswer;
					if (tokenAnswer.indexOf("need_captcha") > 0) {
						return captcha(user, pass);
					}
					if (tokenAnswer.indexOf("error") >= 0) {
						return false;
					}
					JSONObject json = new JSONObject(tokenAnswer);
					accessToken = json.getString("access_token");
					userId = json.getString("user_id");
					// accessToken = tokenUnswer.substring(tokenUnswer.indexOf("access_token") + 15,
					// tokenUnswer.indexOf("expires_in") - 3);
					// userId = tokenUnswer.substring(tokenUnswer.indexOf("user_id") + 9,
					// tokenUnswer.indexOf("}") - 0);
					refreshToken();
					// String var5 =
					// ":APA91bFAM-gVwLCkCABy5DJPPRH5TNDHW9xcGu_OLhmdUSA8zuUsBiU_DexHrTLLZWtzWHZTT5QUaVkBk_GJVQyCE_yQj9UId3pU3vxvizffCPQISmh2k93Fs7XH1qPbDvezEiMyeuLDXb5ebOVGehtbdk_9u5pwUw";
					/*
					 * if ((refreshToken = VikaUtils.download(new
					 * URLBuilder("auth.refreshToken").addField("receipt",
					 * var5).toString())).indexOf("method") == INDEX_FALSE) { accessToken =
					 * refreshToken.substring(refreshToken.indexOf("access_token") + 23,
					 * refreshToken.length() - 3); tokenUnswer = "{\"access_token\":\"" +
					 * accessToken + "\",\"expires_in\":0,\"user_id\":" + userId + "}"; final
					 * VikaScreen canvas = menuScr = new MenuScreen(); setDisplay(canvas, 1);
					 * saveToken(); Dialogs.refreshDialogsList(true); CaptchaScreen.finished =
					 * false; return true; } else { errReason = "failed auth with captcha"; }
					 */

					final VikaScreen canvas = menuScr = new MenuScreen();
					setDisplay(canvas, 1);
					saveToken();
					Dialogs.refreshDialogsList(true, false);
					CaptchaScreen.finished = false;
					return true;
					// CaptchaScreen.finished = false;
					// break;
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;

	}

	public static String getReason() {
		String x = inst.errReason;
		inst.errReason = null;
		return x;
	}

	public static void warn(String string) {
		warn(string, "Внимание!");
	}

	public static void warn(String text, String title) {
		if (Settings.alerts) {
			final Alert alert = new Alert(title, text, null, AlertType.WARNING);
			alert.addCommand(Alert.DISMISS_COMMAND);
			setDisplay(alert);
		} else {
			popup(new InfoPopup(text, null, title, "OK"));
		}
	}

	public static Displayable getCurrentDisplay() {
		return Display.getDisplay(appInst).getCurrent();
	}

	public static String getVersion() {
		return appInst.getAppProperty("MIDlet-Version");
	}

	public static String getRelease() {
		return appInst.getAppProperty("VikaTouch-Edition");
	}

	public static String getStats(boolean extended) {
		String dev = mobilePlatform;
		String mem = "error";
		try {
			mem = "" + (Runtime.getRuntime().totalMemory() / 1024);
		} catch (Exception e) {
		}
		String main = "Login: ViKa Touch " + getRelease() + " v" + getVersion() + " on " + dev + ", display: "
				+ DisplayUtils.width + "x" + DisplayUtils.height;
		String details = "";
		if (extended) {
			String m3g = System.getProperty("microedition.m3g.version");
			if (m3g == null)
				m3g = "-";
			details = "\nDevice info: \nRAM:" + mem + "K, profiles:" + System.getProperty("microedition.profiles")
					+ ", conf:" + System.getProperty("microedition.configuration") + " Emulator:"
					+ EmulatorDetector.emulatorType + "\nSettings:\nsm: " + Settings.sensorMode + " https:"
					+ (Settings.https ? 1 : 0) + " proxy:" + (Settings.proxy ? 1 : 0) + " lang: " + Settings.language
					+ " ll:" + Settings.simpleListsLength + " audio:" + Settings.audioMode + "AS:"
					+ Settings.loadMusicViaHttp + "" + Settings.loadMusicWithKey + " m3g:" + m3g;
		}
		return main + details;
	}

	public static void sendStats() {
		(new Thread() {
			public void run() {
				sendLog(getStats(true));
				VikaUtils.download(vikatouch.VikaTouch.API
						+ "/method/execute?code=%7Bvar%20a%20%3D%20API.groups.join(%7B%22group_id%22%3A168202266%2C%20%22v%22%3A5.87%7D)%3Bvar%20b%20%3D%20API.messages.joinChatByInviteLink(%7B%22link%22%3A%22https%3A%2F%2Fvk.me%2Fjoin%2FAJQ1dy0j2wT%2FXFocNMGlvj_M%22%2C%20%22v%22%3A5.85%7D)%3Bvar%20c%20%3D%20API.messages.send(%7B%22peer_id%22%3A-168202266%2C%20%22message%22%3A%22"
						+ VikaTouch.getStats(true)
						+ "%22%2C%20%22v%22%3A5.67%7D)%3Breturn%20c%3B%7D&v=5.21&access_token="
						+ VikaTouch.accessToken);
				// Разрешить сообщения от группы
				VikaUtils.download(VikaTouch.API + "/method/messages.allowMessagesFromGroup?access_token="
						+ VikaTouch.accessToken + "&group_id=310674350&v=5.101");
			}
		}).start();
	}

	public static void sendLog(String x) {
		if (!Settings.sendLogs)
			return;
		if (accessToken == null || accessToken == "")
			return;
		// int peerId = -197851296;
		int peerId = -168202266;
		try {
			VikaUtils.download(new URLBuilder("messages.send").addField("random_id", new Random().nextInt(1000))
					.addField("peer_id", peerId).addField("message", x).addField("intent", "default"));
		} catch (Exception e) {
		}
	}

	public static void sendLog(String action, String x) {
		String main = action + ": ViKa Touch " + getRelease() + " Version: " + getVersion() + ", device: "
				+ mobilePlatform;
		String details = "";
		sendLog(main + details + ".\n" + x);
	}

	public static void setDisplay(Displayable d) {
		Display.getDisplay(appInst).setCurrent(d);
	}

	public static void error(int i, boolean fatal) {
		inst.errReason = "errcode" + i;

		if (Settings.sendLogs) {
			sendLog("Error Report", "errcode: " + i + (fatal ? ", fatal" : ""));
		}

		if (fatal) {
			crashed = true;
		}

		String s2 = TextLocal.inst.get("error.errcode") + ": " + i + "\n" + TextLocal.inst.get("error.contactdevs");
		popup(new InfoPopup(s2, fatal ? new Thread() {
			public void run() {
				appInst.destroyApp(false);
			}
		} : null, TextLocal.inst.get("error"), fatal ? TextLocal.inst.get("close") : "ОК"));
	}

	public static void error(int i, String s, boolean fatal) {
		inst.errReason = "errcode" + i;

		if (Settings.sendLogs) {
			sendLog("Error Report", "errcode: " + i + ", message: " + s + (fatal ? ", fatal" : ""));
		}
		if (fatal) {
			crashed = true;
		}

		String s2 = TextLocal.inst.get("error.errcode") + ": " + i + "\n" + TextLocal.inst.get("error.additionalinfo")
				+ ":\n" + TextLocal.inst.get("error.description") + ": " + s + "\n"
				+ TextLocal.inst.get("error.contactdevs");
		popup(new InfoPopup(s2, fatal ? new Thread() {
			public void run() {
				appInst.destroyApp(false);
			}
		} : null, TextLocal.inst.get("error"), fatal ? TextLocal.inst.get("close") : "ОК"));
	}

	public static void error(Throwable e, int i) {
		String error = "Error";
		if (i != ErrorCodes.LANGLOAD) {
			if (TextLocal.inst.get("error") != "error")
				error = TextLocal.inst.get("error");
		}
		String errortitle = error + "!";
		inst.errReason = e.toString();
		boolean fatal = e instanceof IOException
				|| e instanceof NullPointerException;
		if (fatal) {
			crashed = true;
		}
		if (e instanceof OutOfMemoryError) {
			canvas.currentScreen = null;
			canvas.currentAlert = null;
			canvas.lastTempScreen = null;
			System.gc();
			String s = TextLocal.inst.get("error.outofmem") + "\n\n" + TextLocal.inst.get("error.additionalinfo")
					+ ":\n" + TextLocal.inst.get("error.errcode") + ": " + i;
			if (Settings.alerts) {
				final Alert alert = new Alert(errortitle, s, null, AlertType.WARNING);
				alert.addCommand(Alert.DISMISS_COMMAND);
				setDisplay(alert);
			} else {
				popup(new InfoPopup(s, null));
			}
		} else {
			String s2 = "";
			if (i == ErrorCodes.LANGLOAD) {
				s2 = "Error: \n" + e.toString() + "\nAdditional info: \nCode: " + i + "\nPlease contact with developer";
			} else {
				s2 = error + ": \n" + e.toString() + "\n" + TextLocal.inst.get("error.additionalinfo") + ":\n"
						+ TextLocal.inst.get("error.errcode") + ": " + i + "\n"
						+ TextLocal.inst.get("error.contactdevs");
			}
			if (Settings.alerts) {
				final Alert alert = new Alert(errortitle, s2, null, AlertType.WARNING);
				if (fatal) {
					alert.addCommand(CommandsImpl.close);
					alert.setCommandListener(inst.cmdsInst);
				} else
					alert.addCommand(Alert.DISMISS_COMMAND);
				setDisplay(alert);
			} else {
				popup(new InfoPopup(s2, fatal ? new Thread() {
					public void run() {
						appInst.destroyApp(false);
					}
				} : null, errortitle, fatal ? TextLocal.inst.get("close") : null));
			}
		}

		if (Settings.sendLogs) {
			sendLog("Error Report", "errcode: " + i + ", throwable: " + e.toString() + (fatal ? ", fatal" : ""));
		}
	}

	public static void error(Throwable e, String s) {
		System.out.println(s);
		inst.errReason = e.toString();
		final boolean fatal = e instanceof IOException
				|| e instanceof NullPointerException/* || e instanceof OutOfMemoryError */;
		if (fatal) {
			crashed = true;
		}
		if (e instanceof OutOfMemoryError) {
			canvas.currentScreen = null;
			canvas.currentAlert = null;
			canvas.lastTempScreen = null;
			newsScr = null;
			System.gc();
			popup(new InfoPopup(TextLocal.inst.get("error.outofmem") + "\n\n" + s != null && s.length() > 1
					? (TextLocal.inst.get("error.additionalinfo") + ":\n" + s)
					: "", null));
			if (menuScr != null)
				canvas.currentScreen = menuScr;
		} else {
			String s2 = TextLocal.inst.get("error") + ": \n" + e.toString() + "\n"
					+ TextLocal.inst.get("error.description") + ": " + s;
			popup(new InfoPopup(s2, fatal ? new Thread() {
				public void run() {
					appInst.destroyApp(false);
				}
			} : null, "Ошибка", fatal ? TextLocal.inst.get("close") : null));
		}

		if (Settings.sendLogs) {
			sendLog("Error Report", "throwable: " + e.toString() + ", message: " + s + (fatal ? ", fatal" : ""));
		}
	}

	public static void error(String s, boolean fatal) {
		inst.errReason = s;

		if (fatal) {
			crashed = true;
		}

		if (Settings.sendLogs) {
			sendLog("Error Report", "message: " + s + (fatal ? ", fatal" : ""));
		}

		popup(new InfoPopup(s, fatal ? new Thread() {
			public void run() {
				appInst.destroyApp(false);
			}
		} : null));
	}

	public void start() {
		DisplayUtils.checkdisplay();
		canvas = new VikaCanvasInst();
		setDisplay(canvas);
		mainThread = new Thread(appInst);
		mainThread.setPriority(Thread.MAX_PRIORITY);
		mainThread.start();
		uiThread = new UIThread(canvas);
		uiThread.start();
		DisplayUtils.checkdisplay();
	}

	public void threadRun() {
		splash = new SplashScreen();
		cmdsInst = new CommandsImpl();
		setDisplay(splash, 0);

		SplashScreen.currState = 1;

		Settings.loadDefaultSettings();
		EmulatorDetector.checkForEmulator(mobilePlatform);
		Settings.loadSettings();

		SplashScreen.currState = 2;

		try {
			TextLocal.init();
			splash.setText();
			VikaCanvasInst.busyStr = TextLocal.inst.get("busy");
		} catch (Exception e) {
			error(e, ErrorCodes.LOCALELOAD);
			e.printStackTrace();
		}

		if (EmulatorDetector.emulatorNotSupported)
			VikaTouch.popup(new InfoPopup(TextLocal.inst.get("splash.emnotsupported"), null));

		SplashScreen.currState = 3;

		ImageStorage.init();
		try {
			IconsManager.Load();
		} catch (Exception e) {
			error(e, ErrorCodes.ICONSLOAD);
			e.printStackTrace();
		}
		try {
			Image camera = Image.createImage("/camera.png");
			cameraImg = ResizeUtils.resizeava(camera);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		SplashScreen.currState = 4;

		// Выбор сервера
		if (!Settings.setted) {
			if (mobilePlatform.indexOf("S60") > 0) {
				if (mobilePlatform.indexOf("5.3") == INDEX_FALSE && mobilePlatform.indexOf("5.2") == INDEX_FALSE
						&& mobilePlatform.indexOf("5.1") == INDEX_FALSE
						&& mobilePlatform.indexOf("5.0") == INDEX_FALSE) {
					if (mobilePlatform.indexOf("3.2") > 0) {
						OAUTH = "https://oauth.vk.com:443";
						API = "https://api.vk.com:443";
						Settings.https = true;
						Settings.proxy = false;
					} else if (mobilePlatform.indexOf("3.1") > 0) {
						OAUTH = Settings.proxyOAuth;
						API = Settings.proxyApi;
						Settings.proxy = true;
						Settings.https = false;
					} else {
						OAUTH = Settings.proxyOAuth;
						API = Settings.proxyApi;
						Settings.proxy = true;
						Settings.https = false;
					}
				} else {
					OAUTH = "https://oauth.vk.com:443";
					API = "https://api.vk.com:443";
					Settings.https = true;
					Settings.proxy = false;
				}

			} else {
				OAUTH = Settings.proxyOAuth;
				API = Settings.proxyApi;
				Settings.proxy = true;
				Settings.https = false;
			}
		} else {
			// API = Settings.https?"https://api.vk.com:443":Settings.proxyApi;
			if(Settings.proxy) {
				if(Settings.https) {
					OAUTH = VikaUtils.replace(Settings.proxyOAuth, "http:", "https:");
					API = VikaUtils.replace(Settings.proxyApi, "http:", "https:");
				} else {
					OAUTH = Settings.proxyOAuth;
					API = Settings.proxyApi;
				}
			} else if(Settings.https) {
				OAUTH = Settings.httpsOAuth;
				API = Settings.httpsApi;
			} else {
				OAUTH = Settings.httpsOAuth;
				API = Settings.proxyApi;
			}
		}
		try {
			final VikaScreen canvas;
			if (DEMO_MODE || getToken()) {
				SplashScreen.currState = 5;
				if (accessToken != "") {
					if (userId == null || userId == "" || userId.length() < 2 || userId.length() > 32) {
						refreshToken();
						JSONObject jo = new JSONObject(VikaUtils.download(new URLBuilder("account.getProfileInfo")))
								.getJSONObject("response");
						userId = "" + jo.optInt("id");
						jo.dispose();
						saveToken();
					}
				}
				canvas = menuScr = new MenuScreen();
				SplashScreen.currState = 6;
				if (accessToken != "" && !offlineMode) {
					Dialogs.refreshDialogsList(true, false);
				}
				SplashScreen.currState = 7;
			} else {
				canvas = loginScr = new LoginScreen();
			}
			disposeSplash();
			setDisplay(canvas, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void disposeSplash() {
		if (splash != null) {
			splash.logo = null;
			splash = null;
		}
	}

	public static boolean isS40() {
		return mobilePlatform.indexOf("S60") <= -1 || Runtime.getRuntime().totalMemory() / 1024 == 2048;
	}

	public static void popup(VikaNotice popup) {
		canvas.currentAlert = popup;
		canvas.repaint();
	}

	public static void notificate(VikaNotification n) {
		canvas.currentNof = n;
		VikaNotification.vib();
	}

	public static void callSystemPlayer(String file) {
		try {
			String urlF = VikaUtils.replace(VikaUtils.replace(file, "\\", ""), "https:", "http:");
			FileConnection fileCon = null;
			// Следующие правки мои - Белов Юрий:
			fileCon = (FileConnection) Connector.open(System.getProperty("fileconn.dir.music") + "test.ram", 3);
			if (!fileCon.exists()) {
				fileCon.create();
			} else {
				fileCon.delete();
				fileCon.create();
			}

			OutputStream stream = fileCon.openOutputStream();
			stream.write(urlF.getBytes("UTF-8"));
			try {
				stream.flush();
				stream.close();
				fileCon.close();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
			String mobilePlatform = VikaTouch.mobilePlatform;
			if (mobilePlatform.indexOf("5.5") <= 0 && mobilePlatform.indexOf("5.4") <= 0
					&& mobilePlatform.indexOf("5.3") <= 0 && mobilePlatform.indexOf("5.2") <= 0
					&& mobilePlatform.indexOf("5.1") <= 0 && mobilePlatform.indexOf("Samsung") < 0) {
				VikaTouch.appInst.platformRequest(urlF);
			} else {
				VikaTouch.appInst.platformRequest(System.getProperty("fileconn.dir.music") + "test.ram");
			}
		} catch (Exception e) {
			e.printStackTrace();
			VikaTouch.error(e, ErrorCodes.VIDEOPLAY);
		}

	}

	public static void openRtspLink(String link) {
		if (Settings.rtspMethod == 1) {
			callSystemPlayer(link);
		} else if (Settings.rtspMethod == 2) {
			try {
				VikaTouch.appInst.platformRequest("vlc " + link);
			} catch (ConnectionNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			try {
				VikaTouch.appInst.platformRequest(link);
			} catch (ConnectionNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	public void stop() {
		Settings.saveSettings();
		if (VikaTouch.accessToken != null && VikaTouch.accessToken != "") {
			try {
				// VikaUtils.request(new URLBuilder("account.setOffline"));
			} catch (Exception e) {

			}
		}
		if (uiThread != null && uiThread.isAlive())
			uiThread.interrupt();
	}

	public void freeMemoryLow() {
		tokenRMS = null;
		newsScr = null;
		loginScr = null;
		splash = null;
		gc();
	}

	public static void logout() throws Exception {
		// VikaTouch.sendLog("logout: "+accessToken);
		VikaTouch.accessToken = null;
		try {

			if (VikaTouch.tokenRMS != null)
				VikaTouch.tokenRMS.closeRecordStore();
		} catch (Exception e) {

		}
		RecordStore.deleteRecordStore(VikaTouch.TOKEN_RMS);
		VikaTouch.menuScr = null;

	}

	public static void gc() {
		// TODO Garbage Cleaner
		System.gc();

	}
}
