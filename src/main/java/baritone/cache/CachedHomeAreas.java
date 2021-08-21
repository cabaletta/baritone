/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.cache;

import baritone.api.cache.ICachedHomeAreas;
import baritone.api.selection.ISelection;
import baritone.api.selection.ISelectionManager;
import baritone.api.utils.BetterBlockPos;
import baritone.selection.Selection;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class CachedHomeAreas implements ICachedHomeAreas {
    private Path directory;
    CachedHomeAreas(Path directory)
    {
        this.directory = directory;
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException ignored) {
            }
        }

    }
    @Override
    public synchronized void save(ISelectionManager manager)
    {
        Path fileName = this.directory.resolve("CoolShit.mp4");
        try (
                FileOutputStream fileOut = new FileOutputStream(fileName.toFile());
                BufferedOutputStream bufOut = new BufferedOutputStream(fileOut);
                DataOutputStream out = new DataOutputStream(bufOut)
        ) {
            out.writeInt(manager.getSelections().length);
            for(ISelection selection : manager.getSelections())
            {
                out.writeInt(selection.min().x);
                out.writeInt(selection.min().y);
                out.writeInt(selection.min().z);
                out.writeInt(selection.max().x);
                out.writeInt(selection.max().y);
                out.writeInt(selection.max().z);
            }
        }catch(IOException e) {e.printStackTrace();}
    }

    @Override
    public ISelection[] load()
    {
        Path fileName = this.directory.resolve("CoolShit.mp4");
        if (!Files.exists(fileName)) {
            return null;
        }
        try (
                FileInputStream fileIn = new FileInputStream(fileName.toFile());
                BufferedInputStream bufIn = new BufferedInputStream(fileIn);
                DataInputStream in = new DataInputStream(bufIn)
        ) {
            int length = in.readInt();
            ISelection[] output = new ISelection[length];

            while (length-- > 0) {
                int minx = in.readInt();
                int miny = in.readInt();
                int minz = in.readInt();
                int maxx = in.readInt();
                int maxy = in.readInt();
                int maxz = in.readInt();
                output[length] = new Selection(new BetterBlockPos(minx, miny, minz), new BetterBlockPos(maxx,maxy,maxz));
                }
            return output;
            }
        catch (IOException ignored) {}
        return null;
    }
    @Override
    public void load(ISelectionManager manager)
    {
        ISelection[] selections = load();
        if(selections == null) return;
        manager.removeAllSelections();
        for(ISelection selection : selections)
            manager.addSelection(selection);
    }
}
