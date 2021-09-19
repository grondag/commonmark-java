package grondag.mcmd;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import grondag.mcmd.node.HtmlBlock;
import grondag.mcmd.node.Node;
import grondag.mcmd.parser.Parser;

public class MarkdownLoader implements SimpleSynchronousResourceReloadListener {
	public static final MarkdownLoader INSTANCE = new MarkdownLoader();

	private static final String FOLDER_NAME  = "markdown";
	private static final int PATH_START = FOLDER_NAME.length() + 1;
	private static final String DEFAULT_LANGUAGE  = "en_us";
	private static final Pattern ID_WORD = Pattern.compile("^<\\?\\s+([a-z0-9:_]+)\\s*.*\\n");
	private static final String OPENING = "^<\\?\\s+[a-z0-9:_]+\\s*\\n+";
	private static final String CLOSING = "\\n*.*\\?>\\s*\\n*$";

	private static final Object2ObjectOpenHashMap<ResourceLocation, Node> TEXTS = new Object2ObjectOpenHashMap<>();

	private final ResourceLocation id = new  ResourceLocation(McMdClient.MOD_ID + ":markdown_loader");

	private final Parser parser = Parser.builder().build();

	public static Node get(ResourceLocation id) {
		return TEXTS.get(id);
	}

	@SuppressWarnings("resource")
	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		TEXTS.clear();
		loadLanguage(resourceManager, DEFAULT_LANGUAGE);

		if(Minecraft.getInstance() != null && !DEFAULT_LANGUAGE.equals(Minecraft.getInstance().options.languageCode)) {
			loadLanguage(resourceManager, Minecraft.getInstance().options.languageCode);
		}
	}

	private void loadLanguage(ResourceManager resourceManager, String languageCode) {
		final String suffix = "." + languageCode;
		final int suffixLength = suffix.length();

		for(final ResourceLocation srcId : resourceManager.listResources(FOLDER_NAME, s -> s.endsWith(suffix))) {
			final String path = srcId.getPath();
			final ResourceLocation targetId = new ResourceLocation(srcId.getNamespace(), path.substring(PATH_START, path.length() - suffixLength));
			final Node node = loadMarkdown(resourceManager, srcId, targetId);

			if (node.getFirstChild() instanceof HtmlBlock) {
				loadMultiNode(node.getFirstChild(), targetId.getNamespace());
			} else {
				TEXTS.put(targetId, node);
			}
		}
	}

	private void loadMultiNode(Node node, String namespace) {
		while(node != null && node instanceof HtmlBlock) {
			final HtmlBlock block = (HtmlBlock) node;
			final String raw = block.getLiteral();
			final Matcher matcher = ID_WORD.matcher(raw);

			if (matcher.find()) {
				final String path = matcher.group(1);
				final ResourceLocation targetId = path.contains(":") ? new ResourceLocation(path) : new ResourceLocation(namespace, path);
				final String text = raw.replaceAll(OPENING, "").replaceAll(CLOSING, "");
				TEXTS.put(targetId, parser.parse(text));
			}

			node = node.getNext();
		}
	}

	private Node loadMarkdown(ResourceManager resourceManager, ResourceLocation srcId, ResourceLocation targetId) {
		try (final Resource src = resourceManager.getResource(srcId)) {
			try(final InputStream inputStream = src.getInputStream()) {
				try(final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
					return parser.parseReader(reader);
				}  catch (final Exception e) {
					throw e;
				}
			}  catch (final Exception e) {
				throw e;
			}
		} catch (final Exception e) {
			McMdClient.LOG.error("Couldn't parse localized markdown {} from {}", targetId, srcId, e);
		}

		return null;
	}

	@Override
	public ResourceLocation getFabricId() {
		return id;
	}
}
