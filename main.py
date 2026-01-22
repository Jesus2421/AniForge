# Crea un programa para ver Anime
import os
import tkinter as tk
from animeflv.animeflv import AnimeFLV
import webbrowser

class AnimeSearcher:
    def __init__(self, master):
        self.master = master
        self.master.title("Anime Searcher")
        self.master.resizable(0,0)
        self.master.iconbitmap(os.path.join(os.path.dirname(__file__), 'logo', 'images.ico'))
        self.master.config(bg="sky blue")
        self.api = AnimeFLV()

        self.search_label = tk.Label(master, text="Escribe el nombre de la serie:",bg="sky blue",fg="white", font=("Arial", 14))
        self.search_label.pack()
        self.search_entry = tk.Entry(master, width=40,font=("Arial", 12))
        self.search_entry.pack()
        self.search_button = tk.Button(master, text="Buscar",bg="black",fg="white",font=("Arial", 12, "bold"),command=self.search_anime)
        self.search_button.pack()

        self.search_label = tk.Label(master, text="Los resultado de su busqueda son:",bg="sky blue",fg="white", font=("Arial", 12,"bold"))
        self.search_label.pack()
        self.result_listbox = tk.Listbox(master, width=50,bg="sky blue",fg="white", font=("Arial", 14))
        self.result_listbox.pack()

        self.result_listbox.bind("<<ListboxSelect>>", self.select_anime)


        self.episode_listbox = tk.Listbox(master, width=50,bg="sky blue",fg="white", font=("Arial", 14))
        self.episode_listbox.pack()

        self.episode_listbox.bind("<<ListboxSelect>>", self.select_episode)

        self.MEGA_button = tk.Button(master, text="MEGA",bg="red",fg="white",font=("Arial", 12, "bold"),command=self.open_link)
        self.MEGA_button.pack()


        self.info = None
        self.anime_id = None
        self.elements = []

    def search_anime(self):
        search_text = self.search_entry.get()
        self.elements = self.api.search(search_text)
        self.result_listbox.delete(0, tk.END)
        for element in self.elements:
            self.result_listbox.insert(tk.END, element.title)

    def select_anime(self, event):
        try:
            selection = self.result_listbox.curselection()[0]
            self.anime_id = self.elements[selection].id
            self.info = self.api.get_anime_info(self.anime_id)
            self.info.episodes.reverse()
            self.episode_listbox.delete(0, tk.END)
            for i, episode in enumerate(self.info.episodes):
                self.episode_listbox.insert(tk.END, f"Episodio {i+1}")
        except Exception as e:
            print(f"Error: {e}")


    def select_episode(self, event):
        try:
            selection = self.episode_listbox.curselection()
            selected_text = self.episode_listbox.get(selection)
            selected_index = int(selected_text.split(" ")[1]) - 1
            capitulo = self.info.episodes[selected_index].id
            results = self.api.get_links(self.anime_id, capitulo)
            self.mega_link = None
            for result in results:
                if result.server == "MEGA":
                        self.mega_link = result.url
                break
        except Exception as e:
            print(f"Error: {e}")
    def open_link(self):
        if self.mega_link:
            webbrowser.open(self.mega_link)

root_window = tk.Tk()
anime_searcher = AnimeSearcher(root_window)
root_window.mainloop()