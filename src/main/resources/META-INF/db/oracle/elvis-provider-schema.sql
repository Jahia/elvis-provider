
    drop table elvis_usage_mapping cascade constraints;

    drop sequence hibernate_sequence;

    create table elvis_usage_mapping (
        id number(10,0) not null,
        assetPath varchar2(255 char),
        componentIdentifier varchar2(255 char),
        propertyName varchar2(255 char),
        mountPointIdentifier varchar2(255 char),
        primary key (id)
    );

    create index componentIdentifier on elvis_usage_mapping (componentIdentifier);

    create index mountPointIdentifier on elvis_usage_mapping (mountPointIdentifier);

    create sequence hibernate_sequence;
