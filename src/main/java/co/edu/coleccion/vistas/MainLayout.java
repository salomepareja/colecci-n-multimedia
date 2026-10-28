package co.edu.coleccion.vistas;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

/**
 * Estructura común de todas las vistas: barra superior y menú lateral.
 * En el celular el menú se esconde y se abre con el botón de hamburguesa.
 */
public class MainLayout extends AppLayout {

    public MainLayout() {
        DrawerToggle botonMenu = new DrawerToggle();

        H1 titulo = new H1("Mi colección multimedia");
        titulo.getStyle().set("font-size", "1.2rem").set("margin", "0");

        SideNav menu = new SideNav();
        menu.addItem(new SideNavItem("Colección", ColeccionView.class, VaadinIcon.FOLDER.create()));

        addToDrawer(new Scroller(menu));
        addToNavbar(botonMenu, titulo);
    }
}
