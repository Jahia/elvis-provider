
    drop table if exists elvis_usage_mapping;

    create table elvis_usage_mapping (
        id integer not null auto_increment,
        assetPath varchar(255),
        componentIdentifier varchar(255),
        mountPointIdentifier varchar(255),
        pageUrl varchar(255),
        propertyName varchar(255),
        primary key (id)
    ) ENGINE=InnoDB;

    create index componentIdentifier on elvis_usage_mapping (componentIdentifier);

    create index propertyName on elvis_usage_mapping (propertyName);
