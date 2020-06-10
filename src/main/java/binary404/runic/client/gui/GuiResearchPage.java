package binary404.runic.client.gui;

import binary404.runic.api.capability.CapabilityHelper;
import binary404.runic.api.capability.IPlayerKnowledge;
import binary404.runic.api.internal.CommonInternals;
import binary404.runic.api.research.*;
import binary404.runic.client.gui.research_extras.Page;
import binary404.runic.client.gui.research_extras.PageImage;
import binary404.runic.client.utils.RenderingUtils;
import binary404.runic.common.config.RecipeConfig;
import binary404.runic.common.core.network.PacketHandler;
import binary404.runic.common.core.network.research.PacketSyncProgressToServer;
import binary404.runic.common.core.util.InventoryUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ResourceLoadProgressGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.crafting.IShapedRecipe;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class GuiResearchPage extends Screen {

    public static LinkedList<ResourceLocation> history = new LinkedList<>();
    protected int paneWidth = 256;
    protected int paneHeight = 181;
    protected double guiMapX;
    protected double guiMapY;
    protected int mouseX = 0;
    protected int mouseY = 0;
    private ResearchEntry research;
    private int currentStage = 0;
    int lastStage = 0;
    boolean hold = false;
    private int page = 0;
    private int maxPages = 0;
    IPlayerKnowledge playerKnowledge;
    int rhash = 0;
    float transX = 0.0f;
    float transY = 0.0f;
    float rotX = 0.0f;
    float rotY = 0.0f;
    float rotZ = 0.0f;
    long lastCheck = 0L;
    float pt;
    ResourceLocation tex1 = new ResourceLocation("runic", "textures/gui/gui_researchbook.png");
    ResourceLocation tex2 = new ResourceLocation("runic", "textures/gui/gui_researchbook_overlay.png");
    ResourceLocation tex3 = new ResourceLocation("runic", "textures/aspects/_back.png");
    ResourceLocation tex4 = new ResourceLocation("runic", "textures/gui/paper.png");
    ResourceLocation dummyResearch = new ResourceLocation("runic", "textures/aspects/_unknown.png");
    ResourceLocation dummyMap = new ResourceLocation("runic", "textures/research/rd_map.png");
    ResourceLocation dummyFlask = new ResourceLocation("runic", "textures/research/rd_flask.png");
    ResourceLocation dummyChest = new ResourceLocation("runic", "textures/research/rd_chest.png");
    int hrx = 0;
    int hry = 0;
    static ResourceLocation shownRecipe;
    int recipePage = 0;
    int recipePageMax = 0;
    private long lastCycle = 0L;
    LinkedHashMap<ResourceLocation, ArrayList> recipeLists = new LinkedHashMap();
    LinkedHashMap<ResourceLocation, ArrayList> recipeOutputs = new LinkedHashMap();
    LinkedHashMap<ResourceLocation, ArrayList> drilldownLists = new LinkedHashMap();
    boolean hasRecipePages;
    boolean renderingCompound = false;
    ArrayList<List> reference = new ArrayList();
    private int cycle = -1;
    boolean allowWithPagePopup = false;
    List tipText = null;
    private static final int PAGEWIDTH = 140;
    private static final int PAGEHEIGHT = 210;
    private static final PageImage PILINE = PageImage.parse("runic:textures/gui/gui_researchbook.png:24:184:95:6:1");
    private static final PageImage PIDIV = PageImage.parse("runic:textures/gui/gui_researchbook.png:28:192:140:6:1");
    private ArrayList<Page> pages = new ArrayList();
    boolean isComplete = false;
    boolean hasAllRequisites = false;
    boolean[] hasItem = null;
    boolean[] hasCraft = null;
    boolean[] hasResearch = null;
    boolean[] hasKnow = null;
    boolean[] hasStats = null;
    public HashMap<Integer, String> keyCache = new HashMap();
    int recipeCycle = 0;


    public GuiResearchPage(ResearchEntry research, ResourceLocation recipe, double x, double y) {
        super(new StringTextComponent("gui_page"));
        this.research = research;
        this.guiMapX = x;
        this.guiMapY = y;
        this.minecraft = Minecraft.getInstance();
        this.playerKnowledge = CapabilityHelper.getKnowledge(this.minecraft.player);
        this.parsePages();
        this.page = 0;
        if (recipe != null) {
            shownRecipe = recipe;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.rotX = 25.0F;
        this.rotY = -45.0F;
    }

    @Override
    public void render(int par1, int par2, float par3) {
        this.hasRecipePages = false;
        long nano = System.nanoTime();
        if (nano > this.lastCheck) {
            this.parsePages();
            this.lastCheck = this.hold ? nano + 250000000L : nano + 2000000000L;
            if (this.currentStage > this.lastStage) {
                this.hold = false;
            }
        }
        this.pt = par3;
        this.renderBackground();
        this.genResearchBackground(par1, par2);
        int sw = (this.width - this.paneWidth) / 2;
        int sh = (this.height - this.paneHeight) / 2;
        if (!history.isEmpty()) {
            int mx = par1 - (sw + 118);
            int my = par2 - (sh + 190);
            if (mx >= 0 && my >= 0 && mx < 20 && my < 12) {
                this.minecraft.fontRenderer.drawStringWithShadow(I18n.format("recipe.return"), par1, par2, 16777215);
            }
        }
    }

    @Override
    public boolean charTyped(char par1_, int par2) {
        System.out.println(par1_ + " " + par2);
        if (par2 == 69 || par1_ == 'e') {
            history.clear();
            if (shownRecipe != null) {
                shownRecipe = null;
                //Minecraft.getMinecraft().player.playSound(ModSounds.page, 0.4f, 1.1f);
            } else {
                //this.minecraft.displayGuiScreen(new GuiResearchBrowser(this.guiMapX, this.guiMapY));
            }
            return true;
        } else if (par2 == 263) {
            this.prevPage();
            return true;
        } else if (par2 == 262) {
            this.nextPage();
            return true;
        } else if (par2 == 259) {
            this.goBack();
            return true;
        }
        return super.charTyped(par1_, par2);
    }

    @Override
    public boolean mouseClicked(double par1, double par2, int par3) {
        this.checkRequisites();
        int var4 = (this.width - this.paneWidth) / 2;
        int var5 = (this.height - this.paneHeight) / 2;
        double mx = par1 - this.hrx;
        double my = par2 - this.hry;
        if (shownRecipe == null && !this.hold && this.hasAllRequisites && mx >= 0 && my >= 0 && mx < 64 && my < 12) {
            PacketHandler.INSTANCE.sendToServer(new PacketSyncProgressToServer(this.research.getKey(), false, true, true));
            //Minecraft.getMinecraft().player.playSound(ModSounds.write, 0.66f, 1.0f);
            this.lastCheck = 0L;
            this.lastStage = this.currentStage;
            this.hold = true;
            this.keyCache.clear();
            this.drilldownLists.clear();
        }
        mx = par1 - (var4 + 205);
        my = par2 - (var5 + 192);
        mx = par1 - (var4 + 38);
        my = par2 - (var5 + 192);
        if (this.recipeLists.size() > 0) {
            int aa = 0;
            int space = Math.min(25, 200 / this.recipeLists.size());
            for (ResourceLocation rk : this.recipeLists.keySet()) {
                mx = par1 - (var4 + 280);
                my = par2 - (var5 - 8 + aa * space);
                if (mx >= 0 && my >= 0 && mx < 30 && my < 16) {
                    shownRecipe = rk.equals((Object) shownRecipe) ? null : rk;
                    history.clear();
                    //Minecraft.getMinecraft().player.playSound(ModSounds.page, 0.7f, 0.9f);
                    break;
                }
                ++aa;
            }
        }
        mx = par1 - (var4 + 205);
        my = par2 - (var5 + 192);
        if (this.hasRecipePages && this.recipePage < this.recipePageMax && mx >= 0 && my >= 0 && mx < 14 && my < 14) {
            ++this.recipePage;
            //Minecraft.getMinecraft().player.playSound(ModSounds.page, 0.7f, 0.9f);
        }
        mx = par1 - (var4 + 38);
        my = par2 - (var5 + 192);
        if (this.hasRecipePages && this.recipePage > 0 && mx >= 0 && my >= 0 && mx < 14 && my < 14) {
            --this.recipePage;
            //Minecraft.getMinecraft().player.playSound(ModSounds.page, 0.7f, 0.9f);
        }
        mx = par1 - (var4 + 261);
        my = par2 - (var5 + 189);
        if (shownRecipe == null && mx >= 0 && my >= 0 && mx < 14 && my < 10) {
            this.nextPage();
        }
        mx = par1 - (var4 - 17);
        my = par2 - (var5 + 189);
        if (shownRecipe == null && mx >= 0 && my >= 0 && mx < 14 && my < 10) {
            this.prevPage();
        }
        mx = par1 - (var4 + 118);
        my = par2 - (var5 + 190);
        if (mx >= 0 && my >= 0 && mx < 20 && my < 12) {
            this.goBack();
        }
        mx = par1 - (var4 + 210);
        my = par2 - (var5 + 190);
        if (this.reference.size() > 0) {
            for (List coords : this.reference) {
                if (par1 < (Integer) coords.get(0) || par2 < (Integer) coords.get(1) || par1 >= (Integer) coords.get(0) + 16 || par2 >= (Integer) coords.get(1) + 16)
                    continue;
                try {
                    //Minecraft.getMinecraft().player.playSound(ModSounds.page, 0.66f, 1.0f);
                } catch (Exception exception) {
                    // empty catch block
                }
                if (shownRecipe != null) {
                    history.push(new ResourceLocation(shownRecipe.getNamespace(), shownRecipe.getPath()));
                }
                shownRecipe = (ResourceLocation) coords.get(2);
                this.recipePage = Integer.parseInt((String) coords.get(3));
                if (!this.drilldownLists.containsKey((Object) shownRecipe)) {
                    this.addRecipesToList(shownRecipe, this.drilldownLists, new LinkedHashMap<ResourceLocation, ArrayList>(), shownRecipe);
                }
                break;
            }
        }
        return super.mouseClicked(par1, par2, par3);
    }

    private void nextPage() {
        if (this.page < this.maxPages - 2) {
            this.page += 2;
            this.lastCycle = 0L;
            this.cycle = -1;
            //Minecraft.getMinecraft().player.playSound(ModSounds.page, 0.66f, 1.0f);
        }
    }

    private void prevPage() {
        if (this.page >= 2) {
            this.page -= 2;
            this.lastCycle = 0L;
            this.cycle = -1;
            //Minecraft.getMinecraft().player.playSound(ModSounds.page, 0.66f, 1.0f);
        }
    }

    private void goBack() {
        if (!history.isEmpty()) {
            //Minecraft.getMinecraft().player.playSound(ModSounds.page, 0.66f, 1.0f);
            shownRecipe = history.pop();
        } else {
            shownRecipe = null;
        }
    }

    protected void genResearchBackground(int par1, int par2) {
        int sw = (this.width - this.paneWidth) / 2;
        int sh = (this.height - this.paneHeight) / 2;
        float var10 = ((float) this.width - (float) this.paneWidth * 1.3f) / 2.0f;
        float var11 = ((float) this.height - (float) this.paneHeight * 1.3f) / 2.0f;
        RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
        this.minecraft.getTextureManager().bindTexture(this.tex1);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((int) 770, (int) 771);
        RenderSystem.pushMatrix();
        RenderSystem.translatef((float) var10, (float) var11, (float) 0.0f);
        RenderSystem.scalef((float) 1.3f, (float) 1.3f, (float) 1.0f);
        this.blit(0, 0, 0, 0, this.paneWidth, this.paneHeight);
        RenderSystem.popMatrix();
        this.reference.clear();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((int) 770, (int) 771);
        int current = 0;
        for (int a = 0; a < this.pages.size(); ++a) {
            if ((current == this.page || current == this.page + 1) && current < this.maxPages) {
                this.drawPage(this.pages.get(a), current % 2, sw, sh - 10, par1, par2);
            }
            if (++current > this.page + 1) break;
        }
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((int) 770, (int) 771);
        RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
        this.minecraft.getTextureManager().bindTexture(this.tex1);
        float bob = MathHelper.sin((float) ((float) this.minecraft.player.ticksExisted / 3.0f)) * 0.2f + 0.1f;
        if (!history.isEmpty()) {
            this.drawTexturedModalRectScaled(sw + 118, sh + 190, 38, 202, 20, 12, bob);
        }
        if (this.page > 0 && shownRecipe == null) {
            this.drawTexturedModalRectScaled(sw - 16, sh + 190, 0, 184, 12, 8, bob);
        }
        if (this.page < this.maxPages - 2 && shownRecipe == null) {
            this.drawTexturedModalRectScaled(sw + 262, sh + 190, 12, 184, 12, 8, bob);
        }
        if (this.tipText != null) {
            RenderingUtils.drawCustomTooltip(this, this.minecraft.fontRenderer, this.tipText, par1, par2 + 12);
            this.tipText = null;
        }
    }

    private void drawPage(Page pageParm, int side, int x, int y, int mx, int my) {
        ResearchStage stage;
        if (this.lastCycle < System.currentTimeMillis()) {
            ++this.cycle;
            this.lastCycle = System.currentTimeMillis() + 1000L;
        }
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((int) 770, (int) 771);
        if (this.page == 0 && side == 0) {
            this.blit(x + 4, y - 7, 24, 184, 96, 4);
            this.blit(x + 4, y + 10, 24, 184, 96, 4);
            int offset = this.minecraft.fontRenderer.getStringWidth(this.research.getLocalizedName());
            if (offset <= 140) {
                this.minecraft.fontRenderer.drawString(this.research.getLocalizedName(), x - 15 + 140 / 2 - offset / 2, y, 2105376);
            } else {
                float vv = 140.0f / (float) offset;
                RenderSystem.pushMatrix();
                RenderSystem.translatef((float) ((float) (x - 15 + 140 / 2) - (float) (offset / 2) * vv), (float) ((float) y + 1.0f * vv), (float) 0.0f);
                RenderSystem.scalef((float) vv, (float) vv, (float) vv);
                this.minecraft.fontRenderer.drawString(this.research.getLocalizedName(), 0, 0, 2105376);
                RenderSystem.popMatrix();
            }
            y += 28;
        }
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((int) 770, (int) 771);
        for (Object content : pageParm.contents) {
            if (content instanceof String) {
                RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
                String ss = ((String) content).replace("~B", "");
                this.minecraft.fontRenderer.drawString(ss, x - 15 + side * 152, y - 6, 0);
                y += this.minecraft.fontRenderer.FONT_HEIGHT;
                if (!((String) content).endsWith("~B"))
                    continue;
                y = (int) ((double) y + (double) this.minecraft.fontRenderer.FONT_HEIGHT * 0.66);
                continue;
            }
            if (!(content instanceof PageImage))
                continue;
            PageImage pi = (PageImage) content;
            RenderSystem.pushMatrix();
            RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
            this.minecraft.textureManager.bindTexture(pi.loc);
            int pad = (140 - pi.aw) / 2;
            RenderSystem.translatef((float) (x - 15 + side * 152 + pad), (float) (y - 5), (float) 0.0f);
            RenderSystem.scalef((float) pi.scale, (float) pi.scale, (float) pi.scale);
            this.blit(0, 0, pi.x, pi.y, pi.w, pi.h);
            RenderSystem.popMatrix();
            y += pi.ah + 2;
        }
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((int) 770, (int) 771);

        if ((stage = this.research.getStages()[this.currentStage]).getRecipes() != null) {
            this.drawRecipeBookmarks(x, mx, my);
        }
        if (this.page == 0 && side == 0 && !this.isComplete) {
            this.drawRequirements(x, mx, my, stage);
        }
        this.renderingCompound = false;
        if (shownRecipe != null) {
            this.drawRecipe(mx, my);
        } else if (stage.getWarp() > 0 && !this.isComplete) {
        }
    }

    private void drawRecipeBookmarks(int x, int mx, int my) {
        Random rng = new Random(this.rhash);
        RenderSystem.pushMatrix();
        int y = (this.height - this.paneHeight) / 2 - 8;
        this.allowWithPagePopup = true;
        if (this.recipeOutputs.size() > 0) {
            int space = Math.min(25, 200 / this.recipeOutputs.size());
            for (ResourceLocation rk : this.recipeOutputs.keySet()) {
                int i;
                List list = this.recipeOutputs.get((Object) rk);
                if (list == null || list.size() <= 0 || list.get(i = this.cycle % list.size()) == null) continue;
                int sh = rng.nextInt(3);
                int le = rng.nextInt(3) + (this.mouseInside(x + 280, y - 1, 30, 16, mx, my) ? 0 : 3);
                this.minecraft.getTextureManager().bindTexture(this.tex1);
                if (rk.equals((Object) shownRecipe)) {
                    RenderSystem.color4f((float) 1.0f, (float) 0.5f, (float) 0.5f, (float) 1.0f);
                } else {
                    RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
                }
                this.blit(x + 280 + sh, y - 1, 120 + le, 232, 28, 16);
                this.blit(x + 280 + sh, y - 1, 116, 232, 4, 16);
                RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
                if (list.get(i) instanceof ItemStack) {
                    this.drawStackAt((ItemStack) list.get(i), x + 287 + sh - le, y - 1, mx, my, false);
                }
                y += space;
            }
        }
        this.allowWithPagePopup = false;
        RenderSystem.popMatrix();
    }

    private void drawRequirements(int x, int mx, int my, ResearchStage stage) {
        int shift;
        int idx;
        int y = (this.height - this.paneHeight) / 2 - 16 + 210;
        RenderSystem.pushMatrix();
        boolean b = false;
        if (stage.getResearch() != null) {
            b = true;
            shift = 24;
            RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 0.25f);
            this.minecraft.getTextureManager().bindTexture(this.tex1);
            this.blit(x - 12, (y -= 18) - 1, 200, 232, 56, 16);
            this.drawPopupAt(x - 15, y, mx, my, "demonic.need.research");
            Object loc = null;
            if (this.hasResearch != null) {
                if (this.hasResearch.length != stage.getResearch().length) {
                    this.hasResearch = new boolean[stage.getResearch().length];
                }
                int ss = 18;
                if (stage.getResearch().length > 6) {
                    ss = 110 / stage.getResearch().length;
                }
                for (int a = 0; a < stage.getResearch().length; ++a) {
                    String k;
                    String key = stage.getResearch()[a];
                    loc = stage.getResearchIcon()[a] != null ? new ResourceLocation(stage.getResearchIcon()[a]) : this.dummyResearch;
                    String text = I18n.format((String) ("demonic.research." + key + ".text"));
                    ResearchEntry re = ResearchCategories.getResearch(key);
                    RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
                    if (re != null && re.getIcons() != null) {
                        int idx2 = (int) (System.currentTimeMillis() / 1000L % (long) re.getIcons().length);
                        loc = re.getIcons()[idx2];
                        text = re.getLocalizedName();
                    } else if (key.startsWith("m_")) {
                        loc = this.dummyMap;
                    } else if (key.startsWith("c_")) {
                        loc = this.dummyChest;
                    } else if (key.startsWith("f_")) {
                        loc = this.dummyFlask;
                    } else {
                        RenderSystem.color4f((float) 0.5f, (float) 0.75f, (float) 1.0f, (float) 1.0f);
                    }
                    RenderSystem.pushMatrix();
                    GL11.glEnable((int) 3042);
                    RenderSystem.blendFunc((int) 770, (int) 771);
                    if (loc instanceof ResourceLocation) {
                        this.minecraft.getTextureManager().bindTexture((ResourceLocation) loc);
                        RenderingUtils.drawTexturedQuadFull(x - 15 + shift, y, this.getBlitOffset());
                    } else if (loc instanceof ItemStack) {
                        GL11.glDisable((int) 2896);
                        GL11.glEnable((int) 32826);
                        GL11.glEnable((int) 2903);
                        GL11.glEnable((int) 2896);
                        this.itemRenderer.renderItemAndEffectIntoGUI((ItemStack) loc, x - 15 + shift, y);
                        GL11.glDisable((int) 2896);
                        RenderSystem.depthMask((boolean) true);
                        GL11.glEnable((int) 2929);
                    }
                    RenderSystem.popMatrix();
                    if (this.hasResearch[a]) {
                        RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
                        this.minecraft.getTextureManager().bindTexture(this.tex1);
                        RenderSystem.disableDepthTest();
                        this.blit(x - 15 + shift + 8, y, 159, 207, 10, 10);
                        RenderSystem.enableDepthTest();
                    }
                    this.drawPopupAt(x - 15 + shift, y, mx, my, text);
                    shift += ss;
                }
            }
        }
        if (stage.getObtain() != null) {
            b = true;
            shift = 24;
            RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 0.25f);
            this.minecraft.getTextureManager().bindTexture(this.tex1);
            this.blit(x - 12, (y -= 18) - 1, 200, 216, 56, 16);
            this.drawPopupAt(x - 15, y, mx, my, "runic.need.obtain");
            RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
            if (this.hasItem != null) {
                if (this.hasItem.length != stage.getObtain().length) {
                    this.hasItem = new boolean[stage.getObtain().length];
                }
                int ss = 18;
                if (stage.getObtain().length > 6) {
                    ss = 110 / stage.getObtain().length;
                }
                for (idx = 0; idx < stage.getObtain().length; ++idx) {
                    ItemStack stack = (ItemStack) stage.getObtain()[idx];
                    this.drawStackAt(stack, x - 15 + shift, y, mx, my, true);
                    if (this.hasItem[idx]) {
                        RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
                        this.minecraft.getTextureManager().bindTexture(this.tex1);
                        RenderSystem.disableDepthTest();
                        this.blit(x - 15 + shift + 8, y, 159, 207, 10, 10);
                        RenderSystem.enableDepthTest();
                    }
                    shift += ss;
                }
            }
        }
        if (stage.getCraft() != null) {
            b = true;
            shift = 24;
            RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 0.25f);
            this.minecraft.getTextureManager().bindTexture(this.tex1);
            this.blit(x - 12, (y -= 18) - 1, 200, 200, 56, 16);
            this.drawPopupAt(x - 15, y, mx, my, "runic.need.craft");
            RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
            if (this.hasCraft != null) {
                if (this.hasCraft.length != stage.getCraft().length) {
                    this.hasCraft = new boolean[stage.getCraft().length];
                }
                int ss = 18;
                if (stage.getCraft().length > 6) {
                    ss = 110 / stage.getCraft().length;
                }
                for (idx = 0; idx < stage.getCraft().length; ++idx) {
                    ItemStack stack = (ItemStack) stage.getCraft()[idx];
                    this.drawStackAt(stack, x - 15 + shift, y, mx, my, true);
                    if (this.hasCraft[idx]) {
                        RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
                        this.minecraft.getTextureManager().bindTexture(this.tex1);
                        RenderSystem.disableDepthTest();
                        this.blit(x - 15 + shift + 8, y, 159, 207, 10, 10);
                        RenderSystem.enableDepthTest();
                    }

                    shift += ss;
                }
            }
        }
        if (b) {
            RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
            this.minecraft.getTextureManager().bindTexture(this.tex1);
            this.blit(x + 4, (y -= 12) - 2, 24, 184, 96, 8);
            if (this.hasAllRequisites) {
                this.hrx = x + 20;
                this.hry = y - 6;
                if (this.hold) {
                    String s = I18n.format("runic.stage.hold");
                    int m = this.minecraft.fontRenderer.getStringWidth(s);
                    this.minecraft.fontRenderer.drawStringWithShadow(s, (float) (x + 52) - (float) m / 2.0f, (float) (y - 4), 16777215);
                } else {
                    if (this.mouseInside(this.hrx, this.hry, 64, 12, mx, my)) {
                        RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
                    } else {
                        RenderSystem.color4f((float) 0.8f, (float) 0.8f, (float) 0.9f, (float) 1.0f);
                    }
                    this.minecraft.getTextureManager().bindTexture(this.tex1);
                    this.blit(this.hrx, this.hry, 84, 216, 64, 12);
                    String s = I18n.format((String) "runic.stage.complete");
                    int m = this.minecraft.fontRenderer.getStringWidth(s);
                    this.minecraft.fontRenderer.drawStringWithShadow(s, (float) (x + 52) - (float) m / 2.0f, (float) (y - 4), 16777215);
                }
            }
        }
        RenderSystem.popMatrix();
    }

    private void drawRecipe(int mx, int my) {
        this.allowWithPagePopup = true;
        RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
        this.minecraft.getTextureManager().bindTexture(this.tex4);
        int x = (this.width - 256) / 2;
        int y = (this.height - 256) / 2;
        RenderSystem.disableDepthTest();
        this.blit(x, y, 0, 0, 255, 255);
        RenderSystem.enableDepthTest();
        List list = this.recipeLists.get((Object) shownRecipe);
        if (list == null || list.size() == 0) {
            list = this.drilldownLists.get((Object) shownRecipe);
        }
        if (list != null && list.size() > 0) {
            Object recipe;
            this.hasRecipePages = list.size() > 1;
            this.recipePageMax = list.size() - 1;
            if (this.recipePage > this.recipePageMax) {
                this.recipePage = this.recipePageMax;
            }
            if ((recipe = list.get(this.recipePage % list.size())) != null) {
                if (recipe instanceof IRecipe) {
                    this.drawCraftingPage(x + 128, y + 128, mx, my, (IRecipe) recipe);
                }
                //Add More Recipes TODO
            }
            if (this.hasRecipePages) {
                this.minecraft.getTextureManager().bindTexture(this.tex1);
                float bob = MathHelper.sin((float) ((float) this.minecraft.player.ticksExisted / 3.0f)) * 0.2f + 0.1f;
                if (this.recipePage > 0) {
                    this.drawTexturedModalRectScaled(x + 40, y + 232, 0, 184, 12, 8, bob);
                }
                if (this.recipePage < this.recipePageMax) {
                    this.drawTexturedModalRectScaled(x + 204, y + 232, 12, 184, 12, 8, bob);
                }
            }
        }
        this.allowWithPagePopup = false;
    }

    private void drawCraftingPage(int x, int y, int mx, int my, IRecipe recipe) {
        String text;
        int offset;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((int) 770, (int) 771);
        if (recipe == null) {
            return;
        }
        RenderSystem.pushMatrix();
        this.minecraft.getTextureManager().bindTexture(this.tex2);
        RenderSystem.pushMatrix();
        RenderSystem.color4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((int) 770, (int) 771);
        RenderSystem.translatef((float) x, (float) y, (float) 0.0f);
        RenderSystem.scalef((float) 2.0f, (float) 2.0f, (float) 1.0f);
        this.blit(-26, -26, 60, 15, 51, 52);
        this.blit(-8, -46, 20, 3, 16, 16);
        RenderSystem.popMatrix();
        this.drawStackAt(recipe.getRecipeOutput(), x - 8, y - 84, mx, my, false);
        if (recipe != null && recipe instanceof IShapedRecipe) {
            text = I18n.format((String) "recipe.type.workbench");
            offset = this.minecraft.fontRenderer.getStringWidth(text);
            this.minecraft.fontRenderer.drawString(text, x - offset / 2, y - 104, 5263440);
            int rw = ((IShapedRecipe) recipe).getRecipeWidth();
            int rh = ((IShapedRecipe) recipe).getRecipeHeight();
            NonNullList items = recipe.getIngredients();
            for (int i = 0; i < rw && i < 3; ++i) {
                for (int j = 0; j < rh && j < 3; ++j) {
                    if (items.get(i + j * rw) == null) continue;
                    Ingredient toRender = (Ingredient) items.get(i + j * rw);
                    this.drawStackAt(toRender.getMatchingStacks()[0], x - 40 + i * 32, y - 40 + j * 32, mx, my, true);
                }
            }
        }
        recipeCycle++;
        RenderSystem.popMatrix();
    }

    void drawPopupAt(int x, int y, int mx, int my, String text) {
        if ((shownRecipe == null || this.allowWithPagePopup) && mx >= x && my >= y && mx < x + 16 && my < y + 16) {
            ArrayList<String> s = new ArrayList<String>();
            s.add(I18n.format(text));
            this.tipText = s;
        }
    }

    void drawPopupAt(int x, int y, int w, int h, int mx, int my, String text) {
        if ((shownRecipe == null || this.allowWithPagePopup) && mx >= x && my >= y && mx < x + w && my < y + h) {
            ArrayList<String> s = new ArrayList<>();
            s.add(I18n.format(text));
            this.tipText = s;
        }
    }

    private void parsePages() {
        String[] imgSplit;
        this.checkRequisites();
        this.pages.clear();
        if (this.research.getStages() == null) {
            return;
        }
        boolean complete = false;
        this.currentStage = CapabilityHelper.getKnowledge(this.minecraft.player).getResearchStage(this.research.getKey()) - 1;
        while (this.currentStage >= this.research.getStages().length) {
            --this.currentStage;
            complete = true;
        }
        if (this.currentStage < 0) {
            this.currentStage = 0;
        }
        ResearchStage stage = this.research.getStages()[this.currentStage];
        ResearchAddendum[] addenda = null;
        if (this.research.getAddenda() != null && complete) {
            addenda = this.research.getAddenda();
        }
        this.generateRecipesLists(stage, addenda);
        String rawText = stage.getTextLocalized();
        if (addenda != null) {
            int ac = 0;
            for (ResearchAddendum addendum : addenda) {
                if (!CapabilityHelper.knowsResearchStrict(this.minecraft.player, addendum.getResearch()))
                    continue;
                TranslationTextComponent text = new TranslationTextComponent("runic.addendumtext", new Object[]{++ac});
                rawText = rawText + "<PAGE>" + text.getFormattedText() + "<BR>" + addendum.getTextLocalized();
            }
        }
        rawText = rawText.replaceAll("<BR>", "~B\n\n");
        rawText = rawText.replaceAll("<BR/>", "~B\n\n");
        rawText = rawText.replaceAll("<LINE>", "~L");
        rawText = rawText.replaceAll("<LINE/>", "~L");
        rawText = rawText.replaceAll("<DIV>", "~D");
        rawText = rawText.replaceAll("<DIV/>", "~D");
        rawText = rawText.replaceAll("<PAGE>", "~P");
        rawText = rawText.replaceAll("<PAGE/>", "~P");
        ArrayList<PageImage> images = new ArrayList<PageImage>();
        for (String s : imgSplit = rawText.split("<IMG>")) {
            int i = s.indexOf("</IMG>");
            if (i < 0) continue;
            String clean = s.substring(0, i);
            PageImage pi = PageImage.parse(clean);
            if (pi == null) {
                rawText = rawText.replaceFirst(clean, "\n");
                continue;
            }
            images.add(pi);
            rawText = rawText.replaceFirst(clean, "~I");
        }
        rawText = rawText.replaceAll("<IMG>", "");
        rawText = rawText.replaceAll("</IMG>", "");
        ArrayList<String> firstPassText = new ArrayList<String>();
        String[] temp = rawText.split("~P");
        for (int a = 0; a < temp.length; ++a) {
            String t = temp[a];
            String[] temp1 = t.split("~D");
            for (int x = 0; x < temp1.length; ++x) {
                String t1 = temp1[x];
                String[] temp2 = t1.split("~L");
                for (int b = 0; b < temp2.length; ++b) {
                    String t2 = temp2[b];
                    String[] temp3 = t2.split("~I");
                    for (int c = 0; c < temp3.length; ++c) {
                        String t3 = temp3[c];
                        firstPassText.add(t3);
                        if (c == temp3.length - 1) continue;
                        firstPassText.add("~I");
                    }
                    if (b == temp2.length - 1) continue;
                    firstPassText.add("~L");
                }
                if (x == temp1.length - 1) continue;
                firstPassText.add("~D");
            }
            if (a == temp.length - 1) continue;
            firstPassText.add("~P");
        }
        ArrayList<String> parsedText = new ArrayList<String>();
        for (String s : firstPassText) {
            List pt1 = this.minecraft.fontRenderer.listFormattedStringToWidth(s, 140);
            for (Object ln : pt1) {
                parsedText.add((String) ln);
            }
        }
        int lineHeight = this.minecraft.fontRenderer.FONT_HEIGHT;
        int heightRemaining = 182;
        int dividerSpace = 0;
        if (!this.isComplete) {
            if (stage.getCraft() != null) {
                heightRemaining -= 18;
                dividerSpace = 15;
            }
            if (stage.getObtain() != null) {
                heightRemaining -= 18;
                dividerSpace = 15;
            }
            if (stage.getResearch() != null) {
                heightRemaining -= 18;
                dividerSpace = 15;
            }
        }
        heightRemaining -= dividerSpace;
        Page page1 = new Page();
        ArrayList tempImages = new ArrayList();
        for (String line : parsedText) {
            if (line.contains("~I")) {
                if (!images.isEmpty()) {
                    tempImages.add(images.remove(0));
                }
                line = "";
            }
            if (line.contains("~L")) {
                tempImages.add(PILINE);
                line = "";
            }
            if (line.contains("~D")) {
                tempImages.add(PIDIV);
                line = "";
            }
            if (line.contains("~P")) {
                heightRemaining = 210;
                this.pages.add(page1.copy());
                page1 = new Page();
                line = "";
            }
            if (!line.isEmpty()) {
                line = line.trim();
                page1.contents.add(line);
                heightRemaining -= lineHeight;
                if (line.endsWith("~B")) {
                    heightRemaining = (int) ((double) heightRemaining - (double) lineHeight * 0.66);
                }
            }
            while (!tempImages.isEmpty() && heightRemaining >= ((PageImage) tempImages.get((int) 0)).ah + 2) {
                heightRemaining -= ((PageImage) tempImages.get((int) 0)).ah + 2;
                page1.contents.add(tempImages.remove(0));
            }
            if (heightRemaining >= lineHeight || page1.contents.isEmpty()) continue;
            heightRemaining = 210;
            this.pages.add(page1.copy());
            page1 = new Page();
        }
        if (!page1.contents.isEmpty()) {
            this.pages.add(page1.copy());
        }
        page1 = new Page();
        heightRemaining = 210;
        while (!tempImages.isEmpty()) {
            if (heightRemaining < ((PageImage) tempImages.get((int) 0)).ah + 2) {
                heightRemaining = 210;
                this.pages.add(page1.copy());
                page1 = new Page();
                continue;
            }
            heightRemaining -= ((PageImage) tempImages.get((int) 0)).ah + 2;
            page1.contents.add(tempImages.remove(0));
        }
        if (!page1.contents.isEmpty()) {
            this.pages.add(page1.copy());
        }
        this.rhash = this.research.getKey().hashCode() + this.currentStage * 50;
        this.maxPages = this.pages.size();
    }

    private void generateRecipesLists(ResearchStage stage, ResearchAddendum[] addenda) {
        this.recipeLists.clear();
        this.recipeOutputs.clear();
        if (stage == null || stage.getRecipes() == null) {
            return;
        }
        for (ResourceLocation rk : stage.getRecipes()) {
            this.addRecipesToList(rk, this.recipeLists, this.recipeOutputs, rk);
        }
        if (addenda == null) {
            return;
        }
        for (ResearchAddendum addendum : addenda) {
            if (addendum.getRecipes() == null || !CapabilityHelper.knowsResearchStrict(this.minecraft.player, addendum.getResearch()))
                continue;
            for (ResourceLocation rk : addendum.getRecipes()) {
                this.addRecipesToList(rk, this.recipeLists, this.recipeOutputs, rk);
            }
        }
    }

    private void addRecipesToList(ResourceLocation rk, LinkedHashMap<ResourceLocation, ArrayList> recipeLists2, LinkedHashMap<ResourceLocation, ArrayList> recipeOutputs2, ResourceLocation rkey) {
        Object recipe = CommonInternals.getCatalogRecipe(rk);
        if (recipe == null) {
            recipe = CommonInternals.getCatalogRecipeFake(rk);
        }
        if (recipe == null) {
            recipe = RecipeConfig.manager.getRecipe((ResourceLocation) rk);
        }
        if (recipe == null) {
            recipe = RecipeConfig.recipeGroups.get(rk.toString());
        }
        if (recipe == null) {
            System.out.println("no recipe found matching " + rk);
            return;
        }
        if (recipe instanceof ArrayList) {
            Iterator<ResourceLocation> iterator = ((ArrayList) recipe).iterator();
            while (iterator.hasNext()) {
                ResourceLocation rl = iterator.next();
                this.addRecipesToList(rl, recipeLists2, recipeOutputs2, rk);
            }
        } else {
            if (!recipeLists2.containsKey((Object) rkey)) {
                recipeLists2.put(rkey, new ArrayList());
                recipeOutputs2.put(rkey, new ArrayList());
            }
            ArrayList list = recipeLists2.get((Object) rkey);
            ArrayList outputs = recipeOutputs2.get((Object) rkey);
            if (recipe instanceof IRecipe) {
                System.out.println("adding IRecipe");
                IRecipe re = (IRecipe) recipe;
                list.add(re);
                outputs.add(re.getRecipeOutput());
            }
        }
    }

    private void checkRequisites() {
        if (this.research.getStages() != null) {
            Object[] c;
            String[] r;
            this.isComplete = this.playerKnowledge.isResearchComplete(this.research.getKey());
            while (this.currentStage >= this.research.getStages().length) {
                --this.currentStage;
            }
            if (this.currentStage < 0) {
                return;
            }
            this.hasAllRequisites = true;
            this.hasItem = null;
            this.hasCraft = null;
            this.hasResearch = null;
            this.hasKnow = null;
            ResearchStage stage = this.research.getStages()[this.currentStage];
            Object[] o = stage.getObtain();
            if (o != null) {
                this.hasItem = new boolean[o.length];
                for (int a = 0; a < o.length; ++a) {
                    ItemStack ts = ItemStack.EMPTY;
                    boolean ore = false;
                    if (o[a] instanceof ItemStack) {
                        ts = (ItemStack) o[a];
                    }
                    this.hasItem[a] = InventoryUtils.isPlayerCarryingAmount(this.minecraft.player, ts, ore);
                    if (this.hasItem[a]) continue;
                    this.hasAllRequisites = false;
                }
            }
            if ((c = stage.getCraft()) != null) {
                this.hasCraft = new boolean[c.length];
                for (int a = 0; a < c.length; ++a) {
                    if (!this.playerKnowledge.isResearchKnown("[#]" + stage.getCraftReference()[a])) {
                        this.hasAllRequisites = false;
                        this.hasCraft[a] = false;
                        continue;
                    }
                    this.hasCraft[a] = true;
                }
            }
            if ((r = stage.getResearch()) != null) {
                this.hasResearch = new boolean[r.length];
                for (int a = 0; a < r.length; ++a) {
                    if (!CapabilityHelper.knowsResearchStrict(this.minecraft.player, r[a])) {
                        this.hasAllRequisites = false;
                        this.hasResearch[a] = false;
                        continue;
                    }
                    this.hasResearch[a] = true;
                }
            }
        }
    }

    private String getCraftingRecipeKey(PlayerEntity player, ItemStack stack) {
        int key = stack.serializeNBT().toString().hashCode();
        if (this.keyCache.containsKey(key)) {
            return this.keyCache.get(key);
        }
        for (ResearchCategory rcl : ResearchCategories.researchCategories.values()) {
            for (ResearchEntry ri : rcl.research.values()) {
                if (ri.getStages() == null) continue;
                for (int a = 0; a < ri.getStages().length; ++a) {
                    ResearchStage stage = ri.getStages()[a];
                    if (stage.getRecipes() == null) continue;
                    for (ResourceLocation rec : stage.getRecipes()) {
                        int result = this.findRecipePage(rec, stack, 0);
                        if (result == -1) continue;
                        String s = rec.toString();
                        s = result == -99 ? new ResourceLocation("UNKNOWN").toString() : s + ";" + result;
                        this.keyCache.put(key, s);
                        return s;
                    }
                }
            }
        }
        this.keyCache.put(key, null);
        return null;
    }

    private int findRecipePage(ResourceLocation rk, ItemStack stack, int start) {
        Object recipe = CommonInternals.getCatalogRecipe(rk);
        if (recipe == null) {
            recipe = CommonInternals.getCatalogRecipeFake(rk);
        }
        if (recipe == null) {
            recipe = RecipeConfig.manager.getRecipe(rk);
        }
        if (recipe == null) {
            recipe = RecipeConfig.recipeGroups.get(rk.toString());
        }
        if (recipe == null) {
            return -1;
        }
        if (recipe instanceof ArrayList) {
            int g = 0;
            Iterator<ResourceLocation> iterator = ((ArrayList) recipe).iterator();
            while (iterator.hasNext()) {
                ResourceLocation rl = iterator.next();
                int q = this.findRecipePage(rl, stack, g);
                if (q >= 0) {
                    return q;
                }
                ++g;
            }
        }
        if (recipe instanceof IRecipe && ((IRecipe) recipe).getRecipeOutput().isItemEqual(stack)) {
            return start;
        }
        return -1;
    }

    public void drawTexturedModalRectScaled(int par1, int par2, int par3, int par4, int par5, int par6, float scale) {
        RenderSystem.pushMatrix();
        float var7 = 0.00390625f;
        float var8 = 0.00390625f;
        Tessellator var9 = Tessellator.getInstance();
        RenderSystem.translatef((float) ((float) par1 + (float) par5 / 2.0f), (float) ((float) par2 + (float) par6 / 2.0f), (float) 0.0f);
        RenderSystem.scalef((float) (1.0f + scale), (float) (1.0f + scale), (float) 1.0f);
        var9.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX);
        var9.getBuffer().pos((double) ((float) (-par5) / 2.0f), (double) ((float) par6 / 2.0f), (double) this.getBlitOffset()).tex(((float) (par3 + 0) * var7), ((float) (par4 + par6) * var8)).endVertex();
        var9.getBuffer().pos((double) ((float) par5 / 2.0f), (double) ((float) par6 / 2.0f), (double) this.getBlitOffset()).tex(((float) (par3 + par5) * var7), ((float) (par4 + par6) * var8)).endVertex();
        var9.getBuffer().pos((double) ((float) par5 / 2.0f), (double) ((float) (-par6) / 2.0f), (double) this.getBlitOffset()).tex(((float) (par3 + par5) * var7), ((float) (par4 + 0) * var8)).endVertex();
        var9.getBuffer().pos((double) ((float) (-par5) / 2.0f), (double) ((float) (-par6) / 2.0f), (double) this.getBlitOffset()).tex(((float) (par3 + 0) * var7), ((float) (par4 + 0) * var8)).endVertex();
        var9.draw();
        RenderSystem.popMatrix();
    }

    boolean mouseInside(int x, int y, int w, int h, int mx, int my) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    void drawStackAt(ItemStack itemstack, int x, int y, int mx, int my, boolean clickthrough) {
        if (itemstack == null)
            return;
        RenderingUtils.renderItemStack(this.minecraft, itemstack, x, y, null);
        if ((shownRecipe == null || this.allowWithPagePopup) && mx >= x && my >= y && mx < x + 16 && my < y + 16 && itemstack != null && !itemstack.isEmpty() && itemstack.getItem() != null) {
            if (clickthrough) {
                String[] sr;
                List<String> addtext = new ArrayList<>();
                for (ITextComponent text : itemstack.getTooltip(this.minecraft.player, (Minecraft.getInstance().gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL))) {
                    addtext.add(text.getFormattedText());
                }
                String ref = this.getCraftingRecipeKey(this.minecraft.player, itemstack);
                if (ref != null && (sr = ref.split(";", 2)) != null && sr.length > 1) {
                    ResourceLocation res = new ResourceLocation(sr[0]);
                    if (res.getPath().equals("UNKNOWN")) {
                        addtext.add((Object) TextFormatting.DARK_RED + "" + (Object) TextFormatting.ITALIC + I18n.format((String) "recipe.unknown"));
                    } else {
                        addtext.add((Object) TextFormatting.BLUE + "" + TextFormatting.ITALIC + I18n.format((String) "recipe.clickthrough"));
                        this.reference.add(Arrays.asList(new Comparable[]{Integer.valueOf(mx), Integer.valueOf(my), res, sr[1]}));
                    }
                }
                this.tipText = addtext;
            } else {
                List<String> toolTip = new ArrayList<>();
                for (ITextComponent text : itemstack.getTooltip(this.minecraft.player, (Minecraft.getInstance().gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL))) {
                    toolTip.add(text.getFormattedText());
                }
                this.tipText = toolTip;
            }
        }
    }
}