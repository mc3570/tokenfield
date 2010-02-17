package org.vaadin.tokenfield;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.vaadin.tokenfield.TokenField.InsertPosition;

import com.vaadin.Application;
import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Form;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Layout;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;

public class TokenfieldDemo extends Application {

    @Override
    public void init() {
        setMainWindow(new DemoWindow());
    }

    class DemoWindow extends Window {
        DemoWindow() {
            // Just add some spacing so it looks nicer
            ((VerticalLayout) getContent()).setSpacing(true);

            {
                /*
                 * This is the most basic use case using all defaults; it's
                 * empty to begin with, the user can enter new tokens.
                 */

                Panel p = new Panel("Basic");
                addComponent(p);

                TokenField f = new TokenField("Add tags");
                p.addComponent(f);
            }

            {
                /*
                 * In this example, most features are exercised. A container
                 * with generated contacts is used. The input has filtering
                 * (a.k.a suggestions) enabled, and the added token button is
                 * configured so that it is in the standard "Name <email>"
                 * -format. New contacts can be added to the container ('address
                 * book'), or added as-is (in which case it's styled
                 * differently).
                 */

                Panel p = new Panel("Full featured");
                p.getContent().setStyleName("black");
                addComponent(p);

                // generate container
                Container tokens = generateTestContainer();

                // we want this to be vertical
                VerticalLayout lo = new VerticalLayout();
                lo.setSpacing(true);

                final TokenField f = new TokenField(lo) {
                    protected void onTokenInput(Object tokenId) {
                        Set<Object> set = (Set<Object>) getValue();
                        Contact c = new Contact("", tokenId.toString());
                        if (set != null && set.contains(c)) {
                            // duplicate
                            getWindow().showNotification(
                                    getTokenCaption(tokenId)
                                            + " is already added");
                        } else {
                            if (!cb.containsId(c)) {
                                // don't add directly,
                                // show custom "add to address book" dialog
                                addWindow(new EditContactWindow(tokenId
                                        .toString(), this));
                            } else {
                                // it's in the 'address book', just add
                                addToken(tokenId);
                            }
                        }
                    }

                    protected void onTokenClicked(final Object tokenId) {
                        getWindow().addWindow(
                                new RemoveWindow((Contact) tokenId, this));
                    }

                    protected void configureTokenButton(Object tokenId,
                            Button button) {
                        super.configureTokenButton(tokenId, button);
                        // custom caption
                        button.setCaption(getTokenCaption(tokenId) + " <"
                                + tokenId + ">");
                        // width
                        button.setWidth("100%");

                        if (!cb.containsId(tokenId)) {
                            // it's not in the address book; style
                            button
                                    .addStyleName(TokenField.STYLE_BUTTON_EMPHAZISED);
                        }
                    }
                };
                f.setStyleName(TokenField.STYLE_TOKENFIELD); // no fake textfield look
                f.setWidth("100%");
                f.setInputWidth("100%");
                f.setContainerDataSource(tokens); // 'address book'
                f.setFilteringMode(ComboBox.FILTERINGMODE_CONTAINS); // suggest
                f.setTokenCaptionPropertyId("name"); // use name in input
                f.setInputPrompt("Enter contact name or new email address");
                f.setRememberNewTokens(false); // we'll do this via the dialog
                p.addComponent(f);
            }

            {
                /*
                 * This example uses to selects to dynamically change the insert
                 * position and the layout used.
                 */

                Panel p = new Panel("Layout and InsertPosition");
                addComponent(p);

                HorizontalLayout controls = new HorizontalLayout();
                p.addComponent(controls);

                // generate container
                Container tokens = generateTestContainer();

                // w/ datasource, no configurator
                final TokenField f = new TokenField();
                f.setContainerDataSource(tokens);
                f.setNewTokensAllowed(false);
                f.setFilteringMode(ComboBox.FILTERINGMODE_CONTAINS);
                f.setInputPrompt("firstname.lastname@example.com");
                p.addComponent(f);

                final NativeSelect lo = new NativeSelect("Layout");
                lo.setImmediate(true);
                lo.addItem(HorizontalLayout.class);
                lo.addItem(VerticalLayout.class);
                lo.addItem(GridLayout.class);
                lo.addItem(CssLayout.class);
                lo.setNullSelectionAllowed(false);
                lo.setValue(f.getLayout().getClass());
                lo.addListener(new ValueChangeListener() {
                    public void valueChange(ValueChangeEvent event) {
                        try {
                            Layout l = (Layout) ((Class) event.getProperty()
                                    .getValue()).newInstance();
                            if (l instanceof GridLayout) {
                                ((GridLayout) l).setColumns(3);
                            }
                            f.setLayout(l);
                        } catch (Exception e) {
                            getMainWindow().showNotification("Ouch!",
                                    "Could not make a " + lo.getValue());
                            lo.setValue(f.getLayout().getClass());
                            e.printStackTrace();
                        }
                    }
                });
                controls.addComponent(lo);

                final NativeSelect ip = new NativeSelect("InsertPosition");
                ip.setImmediate(true);
                ip.addItem(InsertPosition.AFTER);
                ip.addItem(InsertPosition.BEFORE);
                ip.setNullSelectionAllowed(false);
                ip.setValue(f.getTokenInsertPosition());
                ip.addListener(new ValueChangeListener() {
                    public void valueChange(ValueChangeEvent event) {
                        f
                                .setTokenInsertPosition((InsertPosition) ip
                                        .getValue());
                    }
                });
                controls.addComponent(ip);

                final CheckBox cb = new CheckBox("Read-only");
                cb.setImmediate(true);
                cb.setValue(f.isReadOnly());
                cb.addListener(new ValueChangeListener() {
                    public void valueChange(ValueChangeEvent event) {
                        f.setReadOnly(cb.booleanValue());
                    }
                });
                controls.addComponent(cb);
                controls.setComponentAlignment(cb, Alignment.BOTTOM_LEFT);

            }

            {
                Panel p = new Panel("Data binding (property data source)");
                addComponent(p);

                // generate container
                Container tokens = generateTestContainer();

                // just for layout; ListSelect left, TokenField right
                HorizontalLayout lo = new HorizontalLayout();
                lo.setWidth("100%");
                lo.setSpacing(true);
                p.setContent(lo);

                // A regular list select
                ListSelect list = new ListSelect();
                lo.addComponent(list);
                list.setImmediate(true);
                list.setMultiSelect(true);
                list.setContainerDataSource(tokens);

                // TokenField bound to the ListSelect above, CssLayout so that
                // it wraps nicely.
                final TokenField f = new TokenField(new CssLayout());
                f.setContainerDataSource(tokens);
                // f.setNewTokensAllowed(false);
                f.setFilteringMode(ComboBox.FILTERINGMODE_CONTAINS);
                f.setPropertyDataSource(list);
                lo.addComponent(f);
                lo.setExpandRatio(f, 1.0f);
            }
        }
    }

    /**
     * This is the window used to confirm removal
     */
    class RemoveWindow extends Window {
        RemoveWindow(final Contact c, final TokenField f) {
            super("Remove " + c.getName() + "?");

            setStyleName("black");
            setResizable(false);
            center();
            setModal(true);
            setWidth("250px");
            setClosable(false);

            // layout buttons horizontally
            HorizontalLayout hz = new HorizontalLayout();
            addComponent(hz);
            hz.setSpacing(true);
            hz.setWidth("100%");

            Button cancel = new Button("Cancel", new Button.ClickListener() {
                public void buttonClick(ClickEvent event) {
                    f.getWindow().removeWindow(getWindow());
                }
            });
            hz.addComponent(cancel);
            hz.setComponentAlignment(cancel, Alignment.MIDDLE_LEFT);

            Button remove = new Button("Remove", new Button.ClickListener() {
                public void buttonClick(ClickEvent event) {
                    f.removeToken(c);
                    f.getWindow().removeWindow(getWindow());
                }
            });
            hz.addComponent(remove);
            hz.setComponentAlignment(remove, Alignment.MIDDLE_RIGHT);

        }
    }

    /**
     * This is the window used to add new contacts to the 'address book'.
     * It does not do proper validation - you can add weird stuff.
     */
    class EditContactWindow extends Window {
        private Contact contact;

        EditContactWindow(final String t, final TokenField f) {
            super("New Contact");
            if (t.contains("@")) {
                contact = new Contact("", t);
            } else {
                contact = new Contact(t, "");
            }
            setModal(true);
            center();
            setWidth("250px");
            setStyleName("black");
            setResizable(false);

            // Just bind a Form to the Contact -pojo via BeanItem
            Form form = new Form();
            form.setItemDataSource(new BeanItem(contact));
            form.setImmediate(true);
            addComponent(form);

            // layout buttons horizontally
            HorizontalLayout hz = new HorizontalLayout();
            addComponent(hz);
            hz.setSpacing(true);
            hz.setWidth("100%");

            Button dont = new Button("Don't add", new Button.ClickListener() {
                public void buttonClick(ClickEvent event) {
                    if (contact.getEmail()==null||contact.getEmail().length()<1) {
                        contact.setEmail(contact.getName());
                    }
                    f.addToken(contact);
                    f.getWindow().removeWindow(getWindow());
                }
            });
            hz.addComponent(dont);
            hz.setComponentAlignment(dont, Alignment.MIDDLE_LEFT);

            Button add = new Button("Add to contacts",
                    new Button.ClickListener() {
                        public void buttonClick(ClickEvent event) {
                            if (contact.getEmail()==null||contact.getEmail().length()<1) {
                                contact.setEmail(contact.getName());
                            }
                            ((BeanItemContainer) f.getContainerDataSource())
                                    .addBean(contact);
                            f.addToken(contact);
                            f.getWindow().removeWindow(getWindow());
                        }
                    });
            hz.addComponent(add);
            hz.setComponentAlignment(add, Alignment.MIDDLE_RIGHT);

        }
    }

    /* Used to generate example contents */
    private static final String[] firstnames = new String[] { "John", "Mary",
            "Joe", "Sarah", "Jeff", "Jane", "Peter", "Marc", "Robert", "Paula",
            "Lenny", "Kenny", "Nathan", "Nicole", "Laura", "Jos", "Josie",
            "Linus" };
    private static final String[] lastnames = new String[] { "Torvalds",
            "Smith", "Adams", "Black", "Wilson", "Richards", "Thompson",
            "McGoff", "Halas", "Jones", "Beck", "Sheridan", "Picard", "Hill",
            "Fielding", "Einstein" };

    private Container generateTestContainer() {
        BeanItemContainer<Contact> container = new BeanItemContainer<Contact>(
                Contact.class);

        HashSet<String> log = new HashSet<String>();
        Random r = new Random(5);
        for (int i = 0; i < 20;) {
            String fn = firstnames[(int) (r.nextDouble() * firstnames.length)];
            String ln = lastnames[(int) (r.nextDouble() * lastnames.length)];
            String name = fn + " " + ln;
            String email = fn.toLowerCase() + "." + ln.toLowerCase()
                    + "@example.com";

            if (!log.contains(email)) {
                log.add(email);
                container.addBean(new Contact(name, email));
                i++;
            }

        }
        return container;
    }

    /**
     * Example Contact -bean, mostly generated setters/getters.
     */
    public class Contact {
        private String name;
        private String email;

        public Contact(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String toString() {
            return email;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Contact) {
                return email.equals(((Contact) obj).getEmail());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return email.hashCode();
        }

    }
}