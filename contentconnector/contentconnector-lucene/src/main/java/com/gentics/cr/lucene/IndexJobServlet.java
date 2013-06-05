package com.gentics.cr.lucene;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.gentics.cr.CRConfigUtil;
import com.gentics.cr.lucene.indexer.compress.IndexCompressor;
import com.gentics.cr.lucene.indexer.index.LuceneIndexLocation;
import com.gentics.cr.lucene.indexer.index.LuceneSingleIndexLocation;
import com.gentics.cr.lucene.information.SpecialDirectoryRegistry;
import com.gentics.cr.monitoring.MonitorFactory;
import com.gentics.cr.servlet.VelocityServlet;
import com.gentics.cr.util.indexing.IndexController;
import com.gentics.cr.util.indexing.IndexExtension;
import com.gentics.cr.util.indexing.IndexJobQueue;
import com.gentics.cr.util.indexing.IndexLocation;

/**
 * @author Christopher Supnig
 */
public class IndexJobServlet extends VelocityServlet {

	private static final long serialVersionUID = 0002L;
	private static final Logger LOGGER = Logger.getLogger(IndexJobServlet.class);
	protected IndexController indexer;

	public void init(final ServletConfig config) throws ServletException {

		super.init(config);
		this.indexer = initIndexController(config);
	}

	/**
	 * implemented as own method to change executed context.
	 * 
	 * @param config
	 * @return indexController
	 */
	public IndexController initIndexController(final ServletConfig config) {
		return IndexController.get(config.getServletName());
	}

	@Override
	public final void destroy() {
		if (indexer != null) {
			indexer.stop();
		}
	}

	@Override
	public void doService(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

		// starttime
		long s = new Date().getTime();
		// get the objects

		String action = getAction(request);
		String index = request.getParameter("idx");
		if ("download".equals(action)) {
			IndexLocation location = indexer.getIndexes().get(index);
			if (location instanceof LuceneSingleIndexLocation) {
				// set metainformation to response
				response.setContentType("application/x-compressed, application/x-tar");
				response.setHeader("Content-Disposition", "attachment; filename=" + index + ".tar.gz");

				// locate the compressed index
				LuceneSingleIndexLocation indexLocation = (LuceneSingleIndexLocation) location;
				File compressedIndexDirectory = new File(indexLocation.getReopenFilename()).getParentFile().getParentFile();
				File compressedIndexFile = new File(new StringBuilder(compressedIndexDirectory.getAbsolutePath()).append("/").append(index)
						.append(".tar.gz").toString());
				if (!compressedIndexFile.exists()) {
					// compress index if it's not present
					IndexCompressor indexCompressor = new IndexCompressor();
					indexCompressor.compress(index);
				}
				// load the compressed index
				InputStream compressedIndex = new FileInputStream(compressedIndexFile);
				IOUtils.copy(compressedIndex, response.getOutputStream());
			}
			skipRenderingVelocity();
		} else {
			response.setContentType("text/html");
			ConcurrentHashMap<String, IndexLocation> indexTable = indexer.getIndexes();

			setTemplateVariables(request);

			for (Entry<String, IndexLocation> e : indexTable.entrySet()) {
				IndexLocation loc = e.getValue();
				IndexJobQueue queue = loc.getQueue();
				ConcurrentHashMap<String, CRConfigUtil> map = loc.getCRMap();
				if (e.getKey().equalsIgnoreCase(index)) {
					if ("stopWorker".equalsIgnoreCase(action)) {
						queue.pauseWorker();
					}
					if ("startWorker".equalsIgnoreCase(action)) {
						queue.resumeWorker();
					}
					if ("reindex".equalsIgnoreCase(action)) {
						loc.createReindexJob();
					}
					if ("addJob".equalsIgnoreCase(action)) {
						String cr = request.getParameter("cr");
						if ("all".equalsIgnoreCase(cr)) {
							loc.createAllCRIndexJobs();
						} else {
							if (cr != null) {
								CRConfigUtil crc = map.get(cr);
								loc.createCRIndexJob(crc, map);
							}
						}
					}
					if ("addExtensionJob".equalsIgnoreCase(action)) {
						String sExt = request.getParameter("ext");
						try {
							HashMap<String, IndexExtension> extensions = ((LuceneIndexLocation) loc).getExtensions();
							if (extensions.containsKey(sExt)) {
								IndexExtension extension = extensions.get(sExt);
								String job = request.getParameter("job");
								extension.addJob(job);
							}
						} catch (Exception ex) {
							LOGGER.info("Couldn not add extension Job");
						}
					}
				}
			}
			render(response);
		}
		// endtime
		long e = new Date().getTime();
		LOGGER.info("Executiontime for getting " + action + " " + (e - s));
	}

	/**
	 * set variables for velocity template.
	 * @param request .
	 */
	protected final void setTemplateVariables(final HttpServletRequest request) {
		ConcurrentHashMap<String, IndexLocation> indexTable = indexer.getIndexes();
		String nc = "&t=" + System.currentTimeMillis();
		String selectedIndex = request.getParameter("index");
		Long totalMemory = Runtime.getRuntime().totalMemory();
		Long freeMemory = Runtime.getRuntime().freeMemory();
		Long maxMemory = Runtime.getRuntime().maxMemory();

		setTemplateVariable("specialDirs", SpecialDirectoryRegistry.getInstance().getSpecialDirectories());
		setTemplateVariable("indexes", indexTable.entrySet());
		setTemplateVariable("nc", nc);
		setTemplateVariable("selectedIndex", selectedIndex);
		String action = getAction(request);
		if ("report".equalsIgnoreCase(action)) {
			setTemplateVariable("report", MonitorFactory.getSimpleReport());
		}
		setTemplateVariable("action", action);
		setTemplateVariable("maxmemory", maxMemory);
		setTemplateVariable("totalmemory", totalMemory);
		setTemplateVariable("freememory", freeMemory);
		setTemplateVariable("usedmemory", totalMemory - freeMemory);
	}

	/**
	 * Get action parameter from request.
	 * @param request Request to get the action parameter of.
	 * @return String containing the action.
	 */
	protected final String getAction(final HttpServletRequest request) {
		return request.getParameter("action");
	}

}
