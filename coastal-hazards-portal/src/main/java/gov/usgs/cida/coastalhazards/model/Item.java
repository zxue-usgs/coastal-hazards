package gov.usgs.cida.coastalhazards.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gov.usgs.cida.coastalhazards.gson.adapter.DoubleSerializer;
import gov.usgs.cida.coastalhazards.model.ogc.WFSService;
import gov.usgs.cida.coastalhazards.model.ogc.WMSService;
import gov.usgs.cida.coastalhazards.model.summary.Summary;
import gov.usgs.cida.utilities.IdGenerator;
import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
@Entity
@Table(name = "item")
public class Item implements Serializable {
    
    public enum ItemType {
        aggregation,
        data;
    }
    
    public enum Type {
        storms,
        vulnerability,
        historical;
    }

    private static final long serialVersionUID = 1L;
    private static final int doublePrecision = 5;
    
    public static final String ITEM_TYPE = "item_type";
    // matches enum above, needed for annotations
    public static final String DATA_TYPE = "data";
    public static final String AGGREGATION_TYPE = "aggregation";
    
    private String id;
    private ItemType itemType;
    private Bbox bbox;
    private Summary summary;
    private String name;
	private String metadata;
    /** @deprecated or rename to theme */
	private Type type;
	private String attr;
    /** @deprecated */
    private transient Rank rank;
	private WFSService wfsService;
	private WMSService wmsService;
    private List<Item> children;
    
    
    @Id
    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
    
    @Enumerated(EnumType.STRING)
    @Column(name = ITEM_TYPE)
    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }
    
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(columnDefinition = "bbox_id")
	public Bbox getBbox() {
		return bbox;
	}

	public void setBbox(Bbox bbox) {
		this.bbox = bbox;
	}
    
    @OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(columnDefinition = "summary_id")
	public Summary getSummary() {
		return summary;
	}

	public void setSummary(Summary summary) {
		this.summary = summary;
	}
    
    @Column(name = "metadata")
	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}

    @Embedded
	public WFSService getWfsService() {
		return wfsService;
	}

	public void setWfsService(WFSService wfsService) {
		this.wfsService = wfsService;
	}

    @Embedded
	public WMSService getWmsService() {
		return wmsService;
	}

	public void setWmsService(WMSService wmsService) {
		this.wmsService = wmsService;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

    @Enumerated(EnumType.STRING)
	public Item.Type getType() {
		return type;
	}

	public void setType(Item.Type type) {
		this.type = type;
	}

	public String getAttr() {
		return attr;
	}

	public void setAttr(String attr) {
		this.attr = attr;
	}

    /** @deprecated */
    @OneToOne
    @PrimaryKeyJoinColumn(name = "id", referencedColumnName = "id")
    public Rank getRank() {
        return rank;
    }
    
    /** @deprecated */
    public void setRank(Rank rank) {
        this.rank = rank;
    }
    
    @ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "aggregation_children",
			joinColumns = {
        @JoinColumn(name = "aggregation_id", referencedColumnName = "id")},
			inverseJoinColumns = {
		@JoinColumn(name = "item_id", referencedColumnName = "id")})
    public List<Item> getChildren() {
        return children;
    }

    public void setChildren(List<Item> children) {
        this.children = children;
    }

	public String toJSON() {
		return new GsonBuilder()
				.registerTypeAdapter(Double.class, new DoubleSerializer(doublePrecision))
				.create()
				.toJson(this);
	}
    
    public static Item fromJSON(String json) {

		Item node;
		GsonBuilder gsonBuilder = new GsonBuilder();
//        gsonBuilder.registerTypeAdapter(Item.class, new ItemAdapter());
//        gsonBuilder.registerTypeAdapter(Geometry.class, new GeometryDeserializer());
//        gsonBuilder.registerTypeAdapter(Envelope.class, new EnvelopeDeserializer());
//        gsonBuilder.registerTypeAdapter(CoordinateSequence.class, new CoordinateSequenceDeserializer());
		Gson gson = gsonBuilder.create();

		node = gson.fromJson(json, Item.class);
		if (node.getId() == null) {
			node.setId(IdGenerator.generate());
		}
		return node;
	}
}
