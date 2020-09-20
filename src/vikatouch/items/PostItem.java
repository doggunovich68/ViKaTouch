package vikatouch.items;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import org.json.me.JSONObject;

import ru.nnproject.vikaui.popup.InfoPopup;
import ru.nnproject.vikaui.utils.ColorUtils;
import ru.nnproject.vikaui.utils.DisplayUtils;
import ru.nnproject.vikaui.utils.images.IconsManager;
import ru.nnproject.vikaui.utils.text.TextBreaker;
import vikatouch.VikaTouch;
import vikatouch.attachments.Attachment;
import vikatouch.attachments.DocumentAttachment;
import vikatouch.attachments.PhotoAttachment;
import vikatouch.attachments.StickerAttachment;
import vikatouch.attachments.VideoAttachment;
import vikatouch.screens.NewsScreen;
import vikatouch.settings.Settings;
import vikatouch.utils.VikaUtils;
import vikatouch.utils.error.ErrorCodes;
import vikatouch.utils.url.URLBuilder;

public class PostItem
	extends JSONUIItem
{
	
	private JSONObject json2;

	public PostItem(JSONObject json, JSONObject ob)
	{
		super(json);
		json2 = ob;
	}

	public int ownerid;
	public int id;
	
	public int views;
	public int reposts;
	public int likes;
	private boolean liked;
	public boolean canlike;
	
	public String copyright;
	public int replyownerid;
	public int replypostid;
	
	private String avaurl;
	private String[] drawText;
	public String name = "";
	public Image ava;
	
	public boolean isreply;
	private int sourceid;
	private String reposterName;
	private String type;
	private String data;
	private boolean dontLoadAva;
	protected boolean hasPrevImg;
	
	int attH = 0;
	
	public void parseJSON()
	{
		super.parseJSON();
		super.parseAttachments();
		//VikaTouch.sendLog(json2.toString());
		try
		{
			if(text == null || text == "")
			{
				text = fixJSONString(json2.optString("text"));
			}
		}
		catch (Exception e)
		{
			VikaTouch.error(e, ErrorCodes.POSTTEXT);
			e.printStackTrace();
			text = "";
		}
		try
		{
			likes = json2.optJSONObject("likes").optInt("count");
			liked = json2.optJSONObject("likes").optInt("user_likes") == 1;
			reposts = json2.optJSONObject("reposts").optInt("count");
			views = json2.optJSONObject("views").optInt("count");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		

		try
		{
			JSONObject postSource = json2.getJSONObject("post_source");
			data = postSource.optString("data");
		}
		catch (Exception e)
		{
			
		}
		
		type = json2.optString("type"); 
		
		copyright = json2.optString("copyright");
		ownerid = json2.optInt("owner_id");
		sourceid = json2.optInt("source_id");
		id = json2.optInt("id");
		replyownerid = json2.optInt("reply_owner_id");
		replypostid = json2.optInt("reply_post_id");
		if(id == 0)
		{
			copyright = json.optString("copyright");
			ownerid = json.optInt("owner_id");
			id = json.optInt("id");
			replyownerid = json.optInt("reply_owner_id");
			replypostid = json.optInt("reply_post_id");
		}
		//itemDrawHeight = 82;
		isreply = replypostid != 0;
		itemDrawHeight = 72;
		int xx = 0;
		xx = replyownerid;
		if(xx == 0)
			xx = fromid;
		if(xx == 0)
			xx = ownerid;
		if(xx == 0)
			xx = sourceid;
		labelgetnameandphoto:
		{
			if(xx < 0)
			{
				for(int i = 0; i < NewsScreen.groups.length(); i++)
				{
					try
					{
						JSONObject group = NewsScreen.groups.getJSONObject(i);
						final int gid = group.optInt("id");
						if(gid == -xx)
						{
							name = group.optString("name");
							avaurl = fixJSONString(group.optString("photo_50"));
							break labelgetnameandphoto;
						}
					}
					catch (Exception e)
					{
						VikaTouch.error(e, ErrorCodes.POSTAVAGROUPS);
						e.printStackTrace();
					}
				}
			}
		}
		
		boolean b1 = false;
		boolean b2 = false;
		for(int i = 0; i < NewsScreen.profiles.length(); i++)
		{
			try
			{
				JSONObject profile = NewsScreen.profiles.getJSONObject(i);
				int uid = profile.optInt("id");
				if(sourceid <= 0)
				{
					b2 = true;
				}
				if(!b2 && uid == sourceid)
				{
					reposterName = "" + profile.optString("first_name") + " " + profile.optString("last_name");
					b2 = true;
				}
				if(xx < 0)
				{
					b1 = true;
				}
				if(!b1 && uid == xx)
				{
					name = "" + profile.optString("first_name") + " " + profile.optString("last_name");
					b1 = true;
					JSONObject jo2 = new JSONObject(VikaUtils.download(new URLBuilder("users.get").addField("user_ids", ""+profile.optInt("id")).addField("fields", "photo_50"))).getJSONArray("response").getJSONObject(0);
					avaurl = fixJSONString(jo2.optString("photo_50"));
				}
				if(b1 && b2)
				{
					break;
				}
			}
			catch (Exception e)
			{
				VikaTouch.error(e, ErrorCodes.POSTAVAPROFILES);
				e.printStackTrace();
			}
		}
		
		
		
		if(reposterName != null)
		{
			itemDrawHeight += 43;
		}

		
		if(data != null && data.equalsIgnoreCase("profile_photo"))
		{
			text = "обновил фотографию на странице";
		}
		
		drawText = TextBreaker.breakText(text, Font.getFont(0, 0, 8), DisplayUtils.width - 32);
		
		//if(text==null || text.length()<5)
			//VikaTouch.sendLog(json2.toString());
		getRes();
		
		System.gc();
	}
	
	/*
	private void getPhotos() 
	{
		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					if(attachments[0] != null)
					{
						if(attachments[0] instanceof PhotoAttachment)
						{
							hasPrevImg = true;
							try
							{
								((PhotoAttachment)attachments[0]).loadForNews();
								prevImage = ((PhotoAttachment)attachments[0]).renderImg;
							}
							catch (Exception e2)
							{
								e2.printStackTrace();
							}
						}
						if(prevImage != null)
						{
							itemDrawHeight += prevImage.getHeight() + 16;
						}
					}
				}
				catch (Exception e)
				{
					VikaTouch.error(e, ErrorCodes.POSTIMAGE);
					e.printStackTrace();
				}
			}
		}).start();
	}
	*/

	public void paint(Graphics g, int y, int scrolled)
	{
		Font f = Font.getFont(0, 0, 8);
		int fh = f.getHeight();
		int textX = 16;
		int dw = DisplayUtils.width;
		g.setFont(f);
		ColorUtils.setcolor(g, ColorUtils.TEXT);
		
		int cy = 0;
		
		if(ava != null)
		{
			g.drawImage(ava, 10, 5 + y, 0);
			g.drawImage(IconsManager.ac, 10, 5 + y, 0);
		}
		
		ColorUtils.setcolor(g, 5);
		
		if(name!=null) g.drawString(name, 70, y + 5 + 25 - f.getHeight()/2, 0);

		cy += 60;
		if(drawText != null)
		{
			for(int i = 0; i < drawText.length; i++)
			{
				if(drawText[i] != null)
					g.drawString(drawText[i], textX, y + cy + fh*i, 0);
			}
			cy+=fh*(drawText.length+1);
		}

		try
		{
			if(attH>0)
			{
				cy += 5;
				for(int i=0; i<attachments.length; i++)
				{
					Attachment at = attachments[i];
					if(at==null) continue;
					
					if(at instanceof PhotoAttachment)
					{
						PhotoAttachment pa = (PhotoAttachment) at;
						if(pa.renderImg == null)
						{
							if(Settings.isLiteOrSomething) {
								g.drawString("Фотография", textX, y+cy, 0);
							} else
								g.drawString("Не удалось загрузить изображение", textX, y+cy, 0);
						}
						else
						{
							g.drawImage(pa.renderImg, (dw-pa.renderW)/2, y+cy, 0);
						}
					}
					else if(at instanceof VideoAttachment)
					{
						VideoAttachment va = (VideoAttachment) at;
						if(va.renderImg == null)
						{
							if(Settings.isLiteOrSomething)
							{
								g.drawString("Видео", textX, y+cy, 0);
							}
							else
								g.drawString("Не удалось загрузить изображение", textX, y+cy, 0);
						}
						else
						{
							g.drawImage(va.renderImg, (dw-va.renderW)/2, y+cy, 0);
							g.drawString(va.title, textX, y+cy+va.renderH, 0);
						}
					}
					else if(at instanceof DocumentAttachment)
					{
						((DocumentAttachment) at).draw(g, textX, y+cy, dw - textX*2);
					}
					
					cy += at.getDrawHeight();
				}
			}
		}
		catch (Exception e) { }
		
		cy+=10;
		g.drawImage(IconsManager.ico[liked?IconsManager.LIKE_F:IconsManager.LIKE], 24, y+cy, 0);
		g.drawString(String.valueOf(likes), 60, y+cy+12 - fh/2, 0);
		cy+=30;
		itemDrawHeight = cy;
	}
	
	public void loadAtts()
	{
		if(attH<=0)
		{
			attH = 0;
			// prepairing attachments
			try {
				for(int i=0; i<attachments.length; i++)
				{
					Attachment at = attachments[i];
					if(at==null) continue;

					if(at instanceof PhotoAttachment)
					{
						((PhotoAttachment) at).loadForNews();
					}
					if(at instanceof VideoAttachment)
					{
						((VideoAttachment) at).loadForMessage();
					}
					if(at instanceof StickerAttachment)
					{
						int stickerH = DisplayUtils.width > 250 ? 128 : 64;
						attH += stickerH + 5;
					}
					else
					{
						attH += at.getDrawHeight() + 5;
					}
				}
				if(attH != 0) { attH += 5; }
			}
			catch (Exception e)
			{
				attH = 0;
				VikaTouch.sendLog(e.toString());
			}
		}
	}

	public void getRes()
	{
		(new Thread() {
			
			public void run() {
				ava = VikaTouch.cameraImg;
				if(!Settings.dontLoadAvas && avaurl != null && !dontLoadAva)
				{
					try
					{
						dontLoadAva = true;
						ava = VikaUtils.downloadImage(avaurl);
					}
					catch (Exception e)
					{
						ava = VikaTouch.cameraImg;
					}
				}
				loadAtts();
			}
		}).start();
	}
	
	public int getDrawHeight()
	{
		return itemDrawHeight;
	}

	public void tap(int x, int y)
	{
		//VikaTouch.sendLog(json2.toString());
	}

	public void keyPressed(int key)
	{
		
	}
}
