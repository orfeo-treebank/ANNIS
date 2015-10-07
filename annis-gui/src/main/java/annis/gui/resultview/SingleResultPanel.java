/*
 * Copyright 2011 Corpuslinguistic working group Humboldt University Berlin.
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
package annis.gui.resultview;

import annis.CommonHelper;
import annis.gui.MetaDataPanel;
import annis.gui.QueryController;
import annis.gui.objects.PagedResultQuery;
import annis.libgui.InstanceConfig;
import annis.libgui.MatchedNodeColors;
import annis.libgui.PluginSystem;
import annis.libgui.ResolverProvider;
import static annis.model.AnnisConstants.ANNIS_NS;
import static annis.model.AnnisConstants.FEAT_MATCHEDNODE;
import static annis.model.AnnisConstants.FEAT_RELANNIS_NODE;
import annis.model.RelannisNodeFeature;
import annis.resolver.ResolverEntry;
import annis.service.objects.Match;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SDominanceRelation;
import org.corpus_tools.salt.common.SSpanningRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.common.SaltProject;
import org.corpus_tools.salt.core.GraphTraverseHandler;
import org.corpus_tools.salt.core.SFeature;
import org.corpus_tools.salt.core.SGraph.GRAPH_TRAVERSE_TYPE;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.eclipse.emf.common.util.BasicEList;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class SingleResultPanel extends CssLayout implements
  Button.ClickListener, VisualizerContextChanger
{
  private static final long serialVersionUID = 2L;

  private static final String HIDE_KWIC = "hide_kwic";

  private static final String INITIAL_OPEN = "initial_open";

  private static final Resource ICON_RESOURCE = FontAwesome.INFO_CIRCLE;

  private SDocument result;

  private Map<String, String> markedCoveredMap;

  private Map<String, String> markedExactMap;

  private final PluginSystem ps;

  private List<VisualizerPanel> visualizers;

  private final Button btInfo;

  private final List<String> path;

  private String segmentationName;

  private final HorizontalLayout infoBar;

  private final String corpusName;

  private final String documentName;

  private final QueryController queryController;

  private final int resultNumber;

  private final ResolverProvider resolverProvider;

  private final Set<String> visibleTokenAnnos;

  private ProgressBar reloadVisualizer;

  private final ComboBox lftCtxCombo;

  private final ComboBox rghtCtxCombo;

  private final Map<Long, Boolean> visualizerState;

  private static final org.slf4j.Logger log = LoggerFactory.getLogger(
    SingleResultPanel.class);

  private final InstanceConfig instanceConfig;
  
  private PagedResultQuery query;
  private final Match match;

  public SingleResultPanel(final SDocument result, 
    Match match,
    int resultNumber,
    ResolverProvider resolverProvider, PluginSystem ps,
    Set<String> visibleTokenAnnos, String segmentationName,
    QueryController controller, InstanceConfig instanceConfig,
    PagedResultQuery query)
  {
    this.ps = ps;
    this.result = result;
    this.segmentationName = segmentationName;
    this.queryController = controller;
    this.resultNumber = resultNumber;
    this.resolverProvider = resolverProvider;
    this.visibleTokenAnnos = visibleTokenAnnos;
    this.instanceConfig = instanceConfig;
    this.query = query;
    this.match = match;

    calculateHelperVariables();

    setWidth("100%");
    setHeight("-1px");

    infoBar = new HorizontalLayout();
    infoBar.addStyleName("info-bar");
    infoBar.setWidth("100%");
    infoBar.setHeight("-1px");

    Label lblNumber = new Label("" + (resultNumber + 1));
    infoBar.addComponent(lblNumber);
    lblNumber.setSizeUndefined();

    btInfo = new Button();
    btInfo.setStyleName(ValoTheme.BUTTON_BORDERLESS);
    btInfo.setIcon(ICON_RESOURCE);
    btInfo.addClickListener((Button.ClickListener) this);
    infoBar.addComponent(btInfo);

    /**
     * Extract the top level corpus name and the document name of this single
     * result.
     */
    path = CommonHelper.getCorpusPath(result.getGraph(), result);
    Collections.reverse(path);
    corpusName = path.get(0);
    documentName = path.get(path.size() - 1);

    MinMax minMax = getIds(result.getDocumentGraph());

    // build label
    StringBuilder sb = new StringBuilder("Path: ");
    sb.append(StringUtils.join(path, " > "));
    sb.append(" (" + minMax.segName + " ").append(minMax.min);
    sb.append(" - ").append(minMax.max).append(")");

    Label lblPath = new Label(sb.toString());

    lblPath.setWidth("100%");
    lblPath.setHeight("-1px");
    infoBar.addComponent(lblPath);
    infoBar.setExpandRatio(lblPath, 1.0f);
    infoBar.setSpacing(true);

    this.visualizerState = new HashMap<>();

    // init context combox
    lftCtxCombo = new ComboBox();
    rghtCtxCombo = new ComboBox();

    lftCtxCombo.setWidth(50, Unit.PIXELS);
    rghtCtxCombo.setWidth(50, Unit.PIXELS);

    lftCtxCombo.setNullSelectionAllowed(false);
    rghtCtxCombo.setNullSelectionAllowed(false);

    lftCtxCombo.addStyleName(ValoTheme.COMBOBOX_SMALL);
    rghtCtxCombo.addStyleName(ValoTheme.COMBOBOX_SMALL);
    
    IndexedContainer lftCtxContainer = new IndexedContainer();
    IndexedContainer rghtCtxContainer = new IndexedContainer();

    // and a property for sorting
    lftCtxContainer.addContainerProperty("number", Integer.class, 0);
    rghtCtxContainer.addContainerProperty("number", Integer.class, 0);

    for (int i = 0; i < 30; i += 5)
    {
      lftCtxContainer.addItem(i).getItemProperty("number").setValue(i);
      rghtCtxContainer.addItem(i).getItemProperty("number").setValue(i);
    }

    int lftContextIdx = query == null ? 0 : query.getLeftContext();
    lftCtxContainer.addItemAt(lftContextIdx, lftContextIdx);
    lftCtxContainer.sort(new Object[]
    {
      "number"
    }, new boolean[]
    {
      true
    });

    int rghtCtxIdx = query == null ? 0 : query.getRightContext();
    rghtCtxContainer.addItem(rghtCtxIdx);

    rghtCtxContainer.sort(new Object[]
    {
      "number"
    }, new boolean[]
    {
      true
    });

    lftCtxCombo.setContainerDataSource(lftCtxContainer);
    rghtCtxCombo.setContainerDataSource(rghtCtxContainer);

    lftCtxCombo.select(lftContextIdx);
    rghtCtxCombo.select(rghtCtxIdx);

    lftCtxCombo.setNewItemsAllowed(true);
    rghtCtxCombo.setNewItemsAllowed(true);

    lftCtxCombo.setImmediate(true);
    rghtCtxCombo.setImmediate(true);

    lftCtxCombo.setNewItemHandler(new AddNewItemHandler(lftCtxCombo));
    rghtCtxCombo.setNewItemHandler(new AddNewItemHandler(rghtCtxCombo));

    lftCtxCombo.addValueChangeListener(
      new ContextChangeListener(resultNumber, true));
    rghtCtxCombo.addValueChangeListener(
      new ContextChangeListener(resultNumber, false));

    Label leftCtxLabel = new Label("left context: ");
    Label rightCtxLabel = new Label("right context: ");

    leftCtxLabel.setWidth("-1px");
    rightCtxLabel.setWidth("-1px");
    
    HorizontalLayout ctxLayout = new HorizontalLayout();
    ctxLayout.setSpacing(true);
    ctxLayout.addComponents(leftCtxLabel, lftCtxCombo, rightCtxLabel,
      rghtCtxCombo);
    infoBar.addComponent(ctxLayout);

    addComponent(infoBar);
    initVisualizer();
  }
  

  public void setSegmentationLayer(String segmentationName)
  {
    this.segmentationName = segmentationName;

    if (result != null)
    {
      List<SNode> segNodes = CommonHelper.getSortedSegmentationNodes(
        segmentationName,
        result.getDocumentGraph());
      Map<String, Long> markedAndCovered = calculateMarkedAndCoveredIDs(result, segNodes);
      for (VisualizerPanel p : visualizers)
      {
        p.setSegmentationLayer(segmentationName, markedAndCovered);
      }
    }
  }

  public void setVisibleTokenAnnosVisible(SortedSet<String> annos)
  {
    for (VisualizerPanel p : visualizers)
    {
      p.setVisibleTokenAnnosVisible(annos);
    }
  }

  private void calculateHelperVariables()
  {
    markedExactMap = new HashMap<>();
    markedCoveredMap = new HashMap<>();

    if (result != null)
    {
      SDocumentGraph g = result.getDocumentGraph();
      if (g != null)
      {
        for (SNode n : result.getDocumentGraph().getNodes())
        {
          SFeature featMatched = n.getFeature(ANNIS_NS, FEAT_MATCHEDNODE);
          Long matchNum = featMatched == null ? null : featMatched.
            getValue_SNUMERIC();

          if (matchNum != null)
          {
            int color = Math.max(0, Math.min((int) matchNum.longValue() - 1,
              MatchedNodeColors.values().length - 1));
            RelannisNodeFeature feat = RelannisNodeFeature.extract(n);
            if (feat != null)
            {
              markedExactMap.put("" + feat.getInternalID(),
                MatchedNodeColors.values()[color].name());
            }
          }

        }
      } // end if g not null
    } // end if result not null
  }

  private void calulcateColorsForMarkedAndCovered(Map<String, Long> markedAndCovered)
  {
    if (markedAndCovered != null)
    {
      for (Entry<String, Long> markedEntry : markedAndCovered.entrySet())
      {
        int color = Math.max(0, Math.min((int) markedEntry.getValue().
          longValue()
          - 1,
          MatchedNodeColors.values().length - 1));
        SNode n = result.getDocumentGraph().getNode(markedEntry.getKey());
        RelannisNodeFeature feat = RelannisNodeFeature.extract(n);

        if (feat != null)
        {
          markedCoveredMap.put("" + feat.getInternalID(),
            MatchedNodeColors.values()[color].name());
        }
      } // end for each entry in markedAndCoverd
    } // end if markedAndCovered not null
  }

  private Map<String, Long> calculateMarkedAndCoveredIDs(
    SDocument doc, List<SNode> segNodes)
  {
    Map<String, Long> initialCovered = new HashMap<>();

    // add all covered nodes
    for (SNode n : doc.getDocumentGraph().getNodes())
    {
      SFeature featMatched = n.getFeature(ANNIS_NS,
        FEAT_MATCHEDNODE);
      Long match = featMatched == null ? null : featMatched.getValue_SNUMERIC();

      if (match != null)
      {
        initialCovered.put(n.getId(), match);
      }
    }

    // calculate covered nodes
    SingleResultPanel.CoveredMatchesCalculator cmc = new SingleResultPanel.CoveredMatchesCalculator(
      doc.getDocumentGraph(), initialCovered);
    Map<String, Long> covered = cmc.getMatchedAndCovered();

    if (segmentationName != null)
    {
      // filter token
      Map<SToken, Long> coveredToken = new HashMap<>();
      for (Map.Entry<String, Long> e : covered.entrySet())
      {
        SNode n = doc.getDocumentGraph().getNode(e.getKey());
        if (n instanceof SToken)
        {
          coveredToken.put((SToken) n, e.getValue());
        }
      }

      for (SNode segNode : segNodes)
      {
        RelannisNodeFeature featSegNode = (RelannisNodeFeature) segNode.
          getFeature(ANNIS_NS, FEAT_RELANNIS_NODE).getValue();

        if (!covered.containsKey(segNode.getId()))
        {
          long leftTok = featSegNode.getLeftToken();
          long rightTok = featSegNode.getRightToken();

          // check for each covered token if this segment is covering it
          for (Map.Entry<SToken, Long> e : coveredToken.entrySet())
          {
            RelannisNodeFeature featTok = (RelannisNodeFeature) e.getKey().
              getFeature(ANNIS_NS, FEAT_RELANNIS_NODE).getValue();
            long entryTokenIndex = featTok.getTokenIndex();
            if (entryTokenIndex <= rightTok && entryTokenIndex >= leftTok)
            {
              // add this segmentation node to the covered set
              covered.put(segNode.getId(), e.getValue());
              break;
            }
          } // end for each covered token
        } // end if not already contained
      } // end for each segmentation node
    }

    return covered;
  }

  @Override
  public void buttonClick(ClickEvent event)
  {
    if (event.getButton() == btInfo && result != null)
    {
      Window infoWindow = new Window("Info for " + result.getId());

      infoWindow.setModal(false);
      MetaDataPanel meta = new MetaDataPanel(path.get(0), path.get(path.size()
        - 1));
      infoWindow.setContent(meta);
      infoWindow.setWidth("400px");
      infoWindow.setHeight("400px");

      UI.getCurrent().addWindow(infoWindow);
    }
  }

  private void showReloadingProgress()
  {
    //remove the old visualizer
    for (VisualizerPanel v : visualizers)
    {
      this.removeComponent(v);
    }

    // first set loading indicator
    reloadVisualizer = new ProgressBar(1.0f);
    reloadVisualizer.setIndeterminate(true);
    reloadVisualizer.setSizeFull();
    reloadVisualizer.setHeight(150, Unit.PIXELS);
    addComponent(reloadVisualizer);
  }

  private void initVisualizer()
  {
    try
    {
      ResolverEntry[] entries 
        = resolverProvider == null ? new ResolverEntry[0] 
        : resolverProvider.getResolverEntries(result);
      visualizers = new LinkedList<>();
      List<VisualizerPanel> openVisualizers = new LinkedList<>();

      List<SNode> segNodes = CommonHelper.getSortedSegmentationNodes(
        segmentationName,
        result.getDocumentGraph());

      Map<String, Long> markedAndCovered = calculateMarkedAndCoveredIDs(result, segNodes);
      calulcateColorsForMarkedAndCovered(markedAndCovered);

      String resultID = "" + new Random().nextInt(Integer.MAX_VALUE);

      for (int i = 0; i < entries.length; i++)
      {
        String htmlID = "resolver-" + resultNumber + "_" + i;

        VisualizerPanel p = new VisualizerPanel(
          entries[i], result, corpusName, documentName,
          visibleTokenAnnos, markedAndCovered,
          markedCoveredMap, markedExactMap,
          htmlID, resultID, this, segmentationName, ps, instanceConfig);

        visualizers.add(p);
        Properties mappings = entries[i].getMappings();

        // check if there is the visibility of a visualizer changed
        // since it the whole result panel was loaded. If not the entry of the
        // resovler entry is used, for determine the visibility status
        if (visualizerState.containsKey(entries[i].getId()))
        {
          if (visualizerState.get(entries[i].getId()))
          {
            openVisualizers.add(p);
          }
        }
        else
        {
          if (Boolean.parseBoolean(mappings.getProperty(INITIAL_OPEN, "false")))
          {
            openVisualizers.add(p);
          }
        }
      } // for each resolver entry

      // attach visualizer
      for (VisualizerPanel p : visualizers)
      {
        addComponent(p);
      }

      for (VisualizerPanel p : openVisualizers)
      {
        p.toggleVisualizer(true, null);
      }
    }
    catch (RuntimeException ex)
    {
      log.error("problems with initializing Visualizer Panel", ex);
    }
    catch (Exception ex)
    {
      log.error("problems with initializing Visualizer Panel", ex);
    }
  }

  @Override
  public void registerVisibilityStatus(long entryId, boolean status)
  {
    visualizerState.put(entryId, status);
  }

  @Override
  public void changeContext(int resultNumber, int context,
    boolean left)
  {
    //delegates the task to the query controller.
    
    queryController.changeContext(query, match, resultNumber, context, this, left);
  }

  private static class AddNewItemHandler implements AbstractSelect.NewItemHandler
  {

    final private ComboBox combobox;

    public AddNewItemHandler(ComboBox comboBox)
    {
      this.combobox = comboBox;
    }

    @Override
    public void addNewItem(String newValue)
    {

      String ERROR_MESSAGE_HEADER = "Illegal value";

      try
      {
        int i = Integer.parseInt(newValue);

        if (i < 0)
        {
          new Notification(ERROR_MESSAGE_HEADER,
            "<div><p>context &lt; 0 makes no sense</p></div>",
            Notification.Type.WARNING_MESSAGE, true).show(Page.getCurrent());
        }
        else
        {

          combobox.getContainerDataSource().addItem(i).
            getItemProperty("number").setValue(i);

          if (combobox.getContainerDataSource() instanceof IndexedContainer)
          {
            ((IndexedContainer) combobox.getContainerDataSource()).sort(
              new Object[]
              {
                "number"
              }, new boolean[]
              {
                true
              });
          }

          combobox.select(i);
        }
      }
      catch (NumberFormatException ex)
      {
        new Notification(ERROR_MESSAGE_HEADER,
          "<div><p>Only numbers are allowed.</p></div>",
          Notification.Type.WARNING_MESSAGE, true).show(Page.getCurrent());
      }
    }
  }

  private class ContextChangeListener implements
    Property.ValueChangeListener
  {

    int resultNumber;

    boolean left;


    public ContextChangeListener(int resultNumber, boolean left)
    {
      this.resultNumber = resultNumber;
      this.left = left;
    }

    @Override
    public void valueChange(Property.ValueChangeEvent event)
    {
      showReloadingProgress();
      lftCtxCombo.setEnabled(false);
      rghtCtxCombo.setEnabled(false);
      int ctx = Integer.parseInt(event.getProperty().getValue().toString());
      changeContext(resultNumber, ctx, left);
    }
  }

  /**
   * Marks all nodes which are dominated by already marked nodes.
   *
   * 1. Sort ascending all initial marked nodes by the size of the intervall
   * between left and right token index.
   *
   * 2. Traverse the salt document graph with the sorted list of step 1. as root
   * nodes and mark all children with the same match position. Already marked
   * nodes are omitted.
   *
   * Note: The algorithm prevents nested marked nodes to be overwritten. Nested
   * nodes must have a smaller intervall from left to right by default, so this
   * should always work.
   *
   */
  public static class CoveredMatchesCalculator implements GraphTraverseHandler
  {

    private Map<String, Long> matchedAndCovered;

    private long currentMatchPos;

    public CoveredMatchesCalculator(SDocumentGraph graph,
      Map<String, Long> initialMatches)
    {
      this.matchedAndCovered = initialMatches;

      Map<SNode, Long> sortedByOverlappedTokenIntervall = new TreeMap<>(
        new Comparator<SNode>()
        {
          @Override
          public int compare(SNode o1, SNode o2)
          {
            RelannisNodeFeature feat1 = (RelannisNodeFeature) o1.getFeature(
              ANNIS_NS, FEAT_RELANNIS_NODE).getValue();
            RelannisNodeFeature feat2 = (RelannisNodeFeature) o2.getFeature(
              ANNIS_NS, FEAT_RELANNIS_NODE).getValue();

            long leftTokIdxO1 = feat1.getLeftToken();
            long rightTokIdxO1 = feat1.getRightToken();
            long leftTokIdxO2 = feat2.getLeftToken();
            long rightTokIdxO2 = feat2.getRightToken();

            int intervallO1 = (int) Math.abs(leftTokIdxO1 - rightTokIdxO1);
            int intervallO2 = (int) Math.abs(leftTokIdxO2 - rightTokIdxO2);

            if (intervallO1 - intervallO2 != 0)
            {
              return intervallO1 - intervallO2;
            }
            else if (feat1.getLeftToken() - feat2.getRightToken() != 0)
            {
              return (int) (feat1.getLeftToken() - feat2.getRightToken());
            }
            else if (feat1.getRightToken() - feat2.getRightToken() != 0)
            {
              return (int) (feat1.getRightToken() - feat2.getRightToken());
            }
            else
            {
              return (int) (feat1.getInternalID() - feat2.getInternalID());
            }
          }
        });

      for (Map.Entry<String, Long> entry : initialMatches.entrySet())
      {
        SNode n = graph.getNode(entry.getKey());
        sortedByOverlappedTokenIntervall.put(n, entry.getValue());
      }

      currentMatchPos = 1;
      if (initialMatches.size() > 0)
      {
        graph.traverse(new BasicEList<>(sortedByOverlappedTokenIntervall.
          keySet()),
          GRAPH_TRAVERSE_TYPE.TOP_DOWN_DEPTH_FIRST, "CoveredMatchesCalculator",
          (GraphTraverseHandler) this, true);
      }
    }

    @Override
    public void nodeReached(GRAPH_TRAVERSE_TYPE traversalType,
      String traversalId, SNode currNode, SRelation edge, SNode fromNode,
      long order)
    {
      if (fromNode != null
        && matchedAndCovered.containsKey(fromNode.getId()) 
        && currNode != null
        && !matchedAndCovered.containsKey(currNode.getId()))
      {
        currentMatchPos = matchedAndCovered.get(fromNode.getId());
        matchedAndCovered.put(currNode.getId(), currentMatchPos);
      }

    }

    @Override
    public void nodeLeft(GRAPH_TRAVERSE_TYPE traversalType, String traversalId,
      SNode currNode, SRelation edge, SNode fromNode, long order)
    {
    }

    @Override
    public boolean checkConstraint(GRAPH_TRAVERSE_TYPE traversalType,
      String traversalId, SRelation edge, SNode currNode, long order)
    {
      if (edge == null || edge instanceof SDominanceRelation
        || edge instanceof SSpanningRelation)
      {
        return true;
      }
      else
      {
        return false;
      }
    }

    public Map<String, Long> getMatchedAndCovered()
    {
      return matchedAndCovered;
    }
  }

  private static class MinMax
  {

    String segName = "tokens";

    long min;

    long max;

  }

  private MinMax getIds(SDocumentGraph graph)
  {
    List<SToken> sTokens = graph.getTokens();

    MinMax minMax = new MinMax();
    minMax.min = Long.MAX_VALUE;
    minMax.max = Long.MIN_VALUE;

    if (segmentationName == null)
    {
      minMax.segName = "tokens";

      if (sTokens != null)
      {
        for (SToken t : sTokens)
        {
          SFeature feature = t.getFeature(ANNIS_NS,
            FEAT_RELANNIS_NODE);
          if(feature != null && feature.getValue() instanceof RelannisNodeFeature)
          {
            RelannisNodeFeature f = (RelannisNodeFeature) feature.getValue();

            if (minMax.min > f.getTokenIndex())
            {
              minMax.min = f.getTokenIndex();
            }

            if (minMax.max < f.getTokenIndex())
            {
              minMax.max = f.getTokenIndex();
            }
          }
        }
      }
    }
    else
    {
      minMax.segName = segmentationName;

      List<SNode> nodes = CommonHelper.getSortedSegmentationNodes(
        segmentationName, graph);

      for (SNode n : nodes)
      {
        RelannisNodeFeature f = RelannisNodeFeature.extract(n);

        if (minMax.min > f.getSegIndex())
        {
          minMax.min = f.getSegIndex();
        }

        if (minMax.max < f.getSegIndex())
        {
          minMax.max = f.getSegIndex();
        }
      }
    }

    minMax.min++;
    minMax.max++;
    
    return minMax;
  }

  @Override
  public void updateResult(SaltProject p, PagedResultQuery query)
  {
    this.query = query;
    if (p != null
      && p.getCorpusGraphs() != null
      && !p.getCorpusGraphs().isEmpty()
      && p.getCorpusGraphs().get(0) != null
      && p.getCorpusGraphs().get(0).getDocuments() != null
      && !p.getCorpusGraphs().get(0).getDocuments().isEmpty())
    {
      this.result = p.getCorpusGraphs().get(0).getDocuments().get(0);
    }

    removeComponent(reloadVisualizer);
    initVisualizer();

    lftCtxCombo.setEnabled(true);
    rghtCtxCombo.setEnabled(true);
  }

  protected SDocument getResult()
  {
    return result;
  }
  
  
}
