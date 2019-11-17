package grondag.mcmd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;

public class McMdClient implements ClientModInitializer {
	public static final String MOD_ID = "mcmarkdown";
	public static Logger LOG = LogManager.getLogger("MC Markdown");

	@Override
	public void onInitializeClient() {
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(MarkdownLoader.INSTANCE);
	}
}
