# Ruby API for Datavyu
# @author Jesse Lingeman
# @author Shohan Hasan
# Please read the function headers for information on how to use them.


# Licensing information:
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.

require 'java'
require 'csv'
require 'time'
require 'date'
require 'set'
require 'rbconfig'
require 'matrix'

import 'org.datavyu.Datavyu'
import 'org.datavyu.models.db.DataStore'
import 'org.datavyu.models.db.MatrixCellValue'
import 'org.datavyu.models.db.NominalCellValue'
import 'org.datavyu.models.db.TextCellValue'
import 'org.datavyu.models.db.CellValue'
import 'org.datavyu.models.db.Variable'
import 'org.datavyu.models.db.Cell'
import 'org.datavyu.models.db.Argument'
import 'org.datavyu.models.project.Project'
import 'org.datavyu.controllers.SaveController'
import 'org.datavyu.controllers.OpenController'
import 'org.datavyu.controllers.project.ProjectController'

$debug = false
# Prints the specified message if global variable #debug set true.
# @param s item to print to console
# @return nil
def print_debug(*s)
  if $debug == true
    p s
  end
end

# Set $db, this is so that JRuby doesn't decide to overwrite it halfway thru the script.
$db = Datavyu.get_project_controller.get_data_store
$pj = Datavyu.get_project_controller.get_project
$sp = Datavyu.get_view()

# Ruby representation of a spreadsheet cell.
# Generally, the two ways to get access to a cell are:
#   RColumn.cells to get a list of cells from a column
#   RColumn.new_cell to create a blank cell in a column.
# @!attribute ordinal
#   @note Prone to change after saving the column to Datavyu.
#   @return [Fixnum] ordinal number of the cell
# @!attribute onset
#   @return [Fixnum] onset time of the cell in milliseconds
# @!attribute offset
#   @return [Fixnum] offset time of the cell in milliseconds
# @!attribute [rw] arglist
#   @note Use RColumn methods to modify column codes. Changing this list for the cell has no effect on the column.
#   @return [Array<String>] list of codes inherited from parent column.
# @!attribute argvals
#   @note Dangerous to modify this directly since the order of the values must match the order of the code names.
#   @return [Array] list of code values
# @!attribute db_cell
#   @note MODIFY AT OWN RISK.
#   @return native Datavyu object corresponding to this cell.
# @!attribute parent
#   @note MODIFY AT OWN RISK.
#   @return [RColumn] the column this cell belongs to
class RCell
  attr_accessor :ordinal, :onset, :offset, :arglist, :argvals, :db_cell, :parent

  # @!visibility private
  # @note This method is not for general use, it is used only when creating
  #       this variable from the database in the get_column method.
  # Sets up methods that can be used to reference the arguments in
  # the cell.
  # @param argvals (required): Values of the arguments being created
  # @param arglist (required): Names of the arguments being created
  def set_args(argvals, arglist)
    @arglist = arglist
    @argvals = (argvals == '')? arglist.map{ '' } : argvals.map{ |x| x.nil?? '' : x }

    # Add getter/setter methods for each code
    arglist.each_with_index do |arg, i|
      instance_eval "def #{arg}; return argvals[#{i}]; end"
      instance_eval "def #{arg}=(val); argvals[#{i}] = val.to_s; end"
    end
  end

  # Map the specified code names to their values.
  # If no names specified, use self.arglist.
  # @note Onset, offset, and ordinal are returned as Integers; all else are Strings
  # @param codes [Array<String>] (optional): Names of codes.
  # @return [Array] Values of specified codes.
  def get_codes(*codes)
    codes = self.arglist if codes.nil? || codes.empty?
    codes.flatten!

    vals = codes.map do |cname|
      case(cname)
      when 'onset'
        self.onset
      when 'offset'
        self.offset
      when 'ordinal'
        self.ordinal
      else
        @arglist.include?(cname)? self.get_arg(cname) : raise("Failed to get the following code from cell #{self.ordinal} in column #{self.parent}: #{cname}")
      end
    end

    return vals
  end
  alias :get_args :get_codes
  alias :getArgs :get_codes

  # Defines an alias for a code
  # @!visibility private
  # @param i [Integer] index of code to change
  # @param new_name [String] new name for code
  def change_code_name(i, new_name)
    instance_eval "def #{new_name}; return argvals[#{i}]; end"
    instance_eval "def #{new_name}=(val); argvals[#{i}] = val.to_s; end"
  end
  alias :change_arg_name :change_code_name

  # Add specified code to end of arglist.
  # @param new_name [String] name of new code
  # @!visibility private
  def add_code(new_name)
    @argvals << ""
    i = argvals.length - 1
    instance_eval "def #{new_name}; return argvals[#{i}]; end"
    instance_eval "def #{new_name}=(val); argvals[#{i}] = val.to_s; end"
  end
  alias :add_arg :add_code

  # Removes code from arglist and associated value from argvals
  # @param name [String] name of code to remove
  # @return [nil]
  def remove_code(name)
    @argvals.delete(arglist.index(name))
    @arglist.delete(name)
  end
  alias :remove_arg :remove_code

  # Get value of a code
  # @param name [String] name of code
  # @return [String, Integer] value of code
  def get_code(name)
    if %w(onset offset ordinal).include?(name) || @arglist.include?(name)
      return self.send(name)
    else
      raise "Cell does not have code '#{name}'"
    end
    # return argvals[arglist.index(name)] if arglist.include?(name)
  end
  alias :get_arg :get_code

  # Changes the value of an argument in a cell.
  # @param arg [String] name of the argument to be changed
  # @param val [String, Fixnum] value to change the argument to
  # @return [nil]
  # @example
  #       trial = get_column("trial")
  #       trial.cells[0].change_code("onset", 1000)
  #       set_column(trial)
  def change_code(arg, val)
    arg = arg.gsub(/(\W)+/, "").downcase
    if arg == "onset"
      val = val.to_i if val.class == String
      @onset = val
    elsif arg == "offset"
      val = val.to_i if val.class == String
      @offset = val
    elsif arg == "ordinal"
      val = val.to_i if val.class == String
      @ordinal = val
    elsif @arglist.include?(arg)
      for i in 0..arglist.length-1
        if arglist[i] == arg and not arg.nil?
          argvals[i] = val.to_s
        end
      end
    else
      raise "Unable to change code '#{arg}'; no such code found."
    end
  end
  alias :change_arg :change_code

  # Print ordinal, onset, offset, and values of all codes in the cell to console.
  # @param sep [String] seperator used between the arguments
  # @return [nil]
  # @example Print the first cell in the 'trial' column
  #       trial = get_column("trial")
  #       puts trial.cells[0].print_all()
  def print_all(sep="\t")
    print @ordinal.to_s + sep + @onset.to_s + sep + @offset.to_s + sep
    @arglist.each do |arg|
      t = eval "self.#{arg}"
      if t == nil
        v = ""
      else
        v = t
      end
      print v + sep
    end
  end

  # Check if self is nested temporally nested
  # @param outer_cell [RCell]: cell to check nesting against
  # @return [true, false]
  # @example
  #       trial = get_column("trial")
  #       id = get_column("id")
  #       if trial.cells[0].is_within(id.cells[0])
  #           do something
  #       end
  def is_within(outer_cell)
    return (outer_cell.onset <= @onset && outer_cell.offset >= @offset && outer_cell.onset <= @offset && outer_cell.offset >= @onset)
  end

  # Check to see if this cell encases another cell temporally
  # @param inner_cell [RCell] cell to check if contained by this cell
  # @return [true, false]
  # @example
  #       trial = get_column("trial")
  #       id = get_column("id")
  #       if id.cells[0].contains(trial.cells[0])
  #           do something
  #       end
  def contains(inner_cell)
    if (inner_cell.onset >= @onset && inner_cell.offset <= @offset && inner_cell.onset <= @offset && inner_cell.offset >= @onset)
      return true
    else
      return false
    end
  end

  # Duration of this cell (currently defined as offset minus onset)
  # @return [Integer] duration of cell in ms
  # @example
  # 	duration = myCell.duration
  def duration
    return @offset - @onset
  end

  # Override method missing.
  # Check if the method is trying to get/set an arg.
  # If it is, define accessor method and send the method to self.
  # @!visibility private
  def method_missing(m, *args, &block)
    mn = m.to_s
    code = (mn.end_with?('=')) ? mn.chop : mn
    if (@arglist.include?(code))
      index = arglist.index(code)
      instance_eval "def #{code}; return argvals[#{index}]; end"
      instance_eval "def #{code}=(val); argvals[#{index}] = val.to_s; end"
      self.send m.to_sym, *args
    else
      super
    end
  end

  # Check if given time falls within this cell's [onset, offset]
  # @param time time in milliseconds to check
  # @return [true, false] true if the given time is greater-than-or-equal to this cell's onset and less-than-or-equal to this cell's offset
  def spans(time)
    (self.onset <= time) && (self.offset >= time)
  end

  # Check if there is any intersection between this cell and given cell
  # @param cell [RCell] other cell
  # @return [true, false] true if there is any temporal overlap between self and given cell
  # @note If the onset of one cell is the offset of another, the two cells are considered to be overlapping.
  def overlaps_cell(cell)
    cell.spans(self.onset) || cell.spans(self.offset) || self.spans(cell.onset) || self.spans(cell.offset)
  end

  # Check if there is any intersection between this cell and given time range (inclusive).
  # @param [Numeric] range time range
  # @return [true, false] true if there is any temporal overlap between self and given time range
  def overlaps_range(range)
    dummy_cell = RCell.new
    dummy_cell.onset = range.first
    dummy_cell.offset = range.last
    overlaps_cell(dummy_cell)
  end

  # Gives the intersection region between self and given cell
  # @param [RCell] cell other cell
  # @return [Range] time range of intersection
  def overlapping_region(cell)
    r = Range.new([self.onset, cell.onset].max, [self.offset, cell.offset].min)
    return r
  end
end


# Ruby implementation of Datavyu column.
# @!attribute name
#   @return [String] name of the column
# @!attribute cells
#   @return [Array<RCell>] list of cells in this column
# @!attribute arglist
#   @return [Array<String>] names of codes for cell in this column, excluding onset, offset, and ordinal
# @!attribute db_var
#   @note Not intended for general use. Modify at own risk.
#   @return Java object for this column
# @!attribute [rw] hidden
#   @return [true, false] visibility of column in spreadsheet
class RColumn

  attr_accessor :name, :type, :cells, :arglist, :old_args, :dirty, :db_var, :hidden

  def initialize()
    hidden = false
  end

  # Validate code name. Remove special characters and replace
  # @param name string to validate
  # @return [String] validated code name
  # @since 1.3.6
  def self.sanitize_codename(name)
    sanitized = name.gsub(/(\W)+/, "").downcase
    sanitized.gsub(/(^\d{1})/, '_\\1')
    return sanitized
  end

  def convert_argname(arg)
    return RColumn.sanitize_codename(arg)
  end

  # @note This function is not for general use.
  # Creates the cell object in the Variable object.
  # @param newcells (required): Array of cells coming from the database via get_column
  # @param arglist (required): Array of the names of the arguments from the database
  def set_cells(newcells, arglist)
    print_debug "Setting cells"
    @cells = Array.new
    @arglist = Array.new
    arglist.each do |arg|
      # Regex to delete any character not a-z,0-9,or _
      print_debug arg
      if ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9"].include?(arg[0].chr)
        arg = "_" + arg
      end
      @arglist << arg.gsub(/(\W)+/, "").downcase
    end
    if !newcells.nil?
      ord = 0
      newcells.each do |cell|
        ord += 1
        c = RCell.new
        c.onset = cell.getOnset
        c.offset = cell.getOffset
        c.db_cell = cell
        c.parent = @name
        vals = Array.new
        if cell.getVariable.getRootNode.type == Argument::Type::MATRIX
          for val in cell.getCellValue().getArguments
            vals << val.toString
          end
        else
          vals << cell.getCellValue().toString
        end
        c.set_args(vals, @arglist)
        c.ordinal = ord
        @cells << c
      end
    end
  end

  # Creates a new, blank cell at the end of this variable's cell array.
  # If a template cell is provided, copies over onset and offset times and code values for any matching code names.
  # @param cell [RCell] template cell
  # @return [RCell] Reference to the cell that was just created.  Modify the cell using this reference.
  # @example
  #   trial = get_column("trial")
  #   new_cell = trial.new_cell()
  #   new_cell.onset = 1000
  #   set_column(trial)
  def new_cell(cell = nil)
    c = RCell.new
    c.set_args('', @arglist)
    if(cell.nil?)
      c.onset = 0
      c.offset = 0
      c.ordinal = 0
    else
      c.onset = cell.onset
      c.offset = cell.offset
      self.arglist.each do |code|
        c.change_arg(code, cell.get_arg(code)) if cell.arglist.include?(code)
      end
    end
    c.parent = @name
    @cells << c
    return c
  end
  alias :make_new_cell :new_cell
  alias :create_cell :new_cell

  # Sorts cells and saves column's cells by ascending onset times.
  # @return nil
  def sort_cells()
    cells.sort! { |a, b| a.onset <=> b.onset }
  end

  # Changes the name of a code. Updates the name for all cells in the column
  # @param old_name the name of the argument you want to change
  # @param new_name the name you want to change old_name to
  # @return nil
  def change_code_name(old_name, new_name)
    i = @old_args.index(old_name)
    @old_args[i] = new_name
    if ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9"].include?(old_name[1].chr)
      old_name = "_" + old_name
    end
    old_name = old_name.gsub(/(\W)+/, "").downcase

    i = @arglist.index(old_name)
    @arglist[i] = new_name
    for cell in @cells
      cell.change_arg_name(i, new_name)
    end

    @dirty = true
  end
  alias :change_arg_name :change_code_name

  # Add a code to this column. Updates all cells in column with new code.
  # @param [String] name the name of the new code
  # @return nil
  def add_code(name)
    @old_args << name
    if ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9"].include?(name[1].chr)
      name = "_" + name
    end
    name = name.gsub(/(\W)+/, "").downcase

    @arglist << name
    for cell in @cells
      cell.add_arg(name)
    end

    @dirty = true
  end
  alias :add_arg :add_code

  # Remove a code from this column. Updates all cells in column.
  # @param [String] name the name of the code to remove
  # @return nil
  def remove_code(name)
    @old_args.delete(name)

    name = name.gsub(/(\W)+/, "").downcase
    @arglist.delete(name)

    for cell in @cells
      cell.remove_arg(name)
    end

    @dirty = true
  end
  alias :remove_arg :remove_code

  # Set hidden state of this column
  # @param value [true, false] true to hide column in spreadsheet, false to show
  # @return nil
  def set_hidden(value)
    @hidden = value
  end

  # Resamples the cells of this column using given step size.
  # Optionally can specify the start and end time-points.
  # @param [Integer] step step size to resample with, in milliseconds
  # @param [Hash] opts options
  # @option opts [String] :column_name (self.name) name of returned column
  # @option opts [Integer] :start_time (earliest onset) time to start resampling from, in milliseconds
  # @option opts [Integer] :stop_time (latest offset) time to stop resampling at, in milliseconds
  # @return [RColumn] new column with resampled cells
  # @note Undefined behavior for columns whose cells overlap with each other.
  # @since 1.3.6
  def resample(step, opts={})
    @resample_defaults = {
      :column_name => self.name,
      :start_time => :earliest,
      :stop_time => :latest
    }

    opts = @resample_defaults.merge(opts)
    if opts[:start_time] == :earliest
      opts[:start_time] = @cells.map(&:onset).min
    end
    if opts[:stop_time] == :latest
      opts[:stop_time] = @cells.map(&:offset).max
    end

    # Construct new column
    ncol = new_column(opts[:column_name], self.arglist)
    # Construct new cells spanning range.
    ( (opts[:start_time])..(opts[:stop_time]) ).step(step) do |time|
      ncell = ncol.new_cell
      ncell.onset = time
      ncell.offset = time + step - 1

      # Find overlapping cells from self in this time region
      overlap_cells = self.cells.select{ |x| x.overlaps_cell(ncell) }
      # if overlap_cells.empty?
      #   puts "no source cell for time #{time}"
      #   next
      # end
      next if overlap_cells.empty? # no source cell

      # Map each to their intersecting region and find the one with the largest duration.
      sorted_by_intersection =  overlap_cells.sort do |x, y|
        r1 = x.overlapping_region(ncell)
        d1 = r1.last - r1.first

        r2 = y.overlapping_region(ncell)
        d2 = r2.last - r2.first

        d2 <=> d1 # largest first
      end
      winner = sorted_by_intersection.first

      ncell.arglist.each do |code|
        ncell.change_code(code, winner.get_code(code))
      end
      # p ncol.cells.size
    end
    return ncol
  end
end

# Patch Matrix class with setter method.  See fmendez.com/blog
class Matrix
  # Add setter method of form matrix[0][0] = 1
  # @param row row index
  # @param column column index
  # @param value new value
  # @return nil
  def []=(row, column, value)
    @rows[row][column] = value
  end
end

# Class for keeping track of the agreement table for one code.
# !@attr table
#   @return [Matrix] contingency table of values
# !@attr codes
#   @return [Array<String>] list of code valus; indices serve as keys for table
class CTable
  attr_accessor :table, :codes

  def initialize(*values)
    raise "CTable must have at least 2 valid values. Got : #{values}" if values.size<2
    @codes = values
    @table = Matrix.zero(values.size)
  end

  # Add a code pair.  Order always pri,rel. Increments the appropriate index of the table by 1.
  # @param pri_value primary coder's value
  # @param rel_value reliability coder's value
  # @return nil
  def add(pri_value, rel_value)
    pri_idx = @codes.index(pri_value)
    raise "Invalid primary value: #{pri_value}" if pri_idx.nil?
    rel_idx = @codes.index(rel_value)
    raise "Invalid reliability value: #{rel_value}" if rel_idx.nil?

    @table[pri_idx, rel_idx] += 1
  end

  # Compute kappa
  # @return simple kappa score
  def kappa
    agree = @table.trace
    total = self.total
    efs = self.efs
    k = (agree-efs)/(total-efs)
    return k
  end

  # Return the expected frequency of agreement by chance for the given index
  # @param idx [Integer] index in codes
  # @return [Fixnum] agreement by chance
  def ef(idx)
    raise "Index out of bounds: requested #{idx}, have #{@codes.size}." if idx >= @codes.size

    # The expected frequency is (row_total * column_total)/matrix_total
    row_total = @table.row(idx).to_a.reduce(:+)
    col_total = @table.column(idx).to_a.reduce(:+)
    ret = (row_total * col_total)/self.total.to_f
    return ret
  end

  # Return the sum of the expected frequency of agreement by chance for all indices in table
  # @return [Fixnum] sum of a agreement by chance
  def efs
    sum = 0
    for idx in 0..@codes.size-1
      sum += self.ef(idx).to_f
    end
    return sum
  end

  # Return the sum of all elements in matrix table
  # @return [Integer] sum of matrix elements
  def total
    v = Matrix.row_vector([1] * @codes.size) # row vector of 1s
    vt = v.t  # column vector of 1s
    ret = (v * @table * vt)
    return ret[0,0]
  end

  # Table to String
  # Return formatted string to display the table
  # @return [String] tab-delimited string showing values in contingency table
  def to_s
    str = "\t" + codes.join("\t") + "\n"
    for i in 0..@codes.size-1
      str << @codes[i] + "\t"
      for j in 0..@codes.size-1
        str << @table[i,j].to_s + "\t"
      end
      str << "\n"
    end
    return str
  end
end


# Compute Cohen's kappa from the given primary and reliability columns.
# @param pri_col [RColumn, String] primary coder's column
# @param rel_col [RColumn, String] reliability coder's column
# @param codes [Array<String>] codes to compute scores for
# @return [Hash<String, Fixnum>] mapping from code names to kappa values
# @return [Hash<String, Matrix>] mapping fromm code names to contingency tables
# @example
#     primary_column_name = 'trial'
#     reliability_column_name = 'trial_rel'
#     codes_to_compute = ['condition', 'result']
#     kappas, tables = compute_kappa(colPri, colRel, codes_to_compute)
#     kappas.each_pair { |code, k| puts "#{code}: #{k}" }
def compute_kappa(pri_col, rel_col, *codes)
  codes = pri_col.arglist if codes.nil? || codes.empty?
  raise "No codes!" if codes.empty?

  pri_col = get_column(pri_col) if pri_col.class == String
  rel_col = get_column(rel_col) if rel_col.class == String
  codes.flatten!

  raise "Invalid parameters for getKappa()" unless (pri_col.class==RColumn && rel_col.class==RColumn)

  # Get the list of observed values in each cell, per code
  cells = pri_col.cells + rel_col.cells

  # Build a hashmap from the list of codes to all observed values for that code
  # across primary and reliability cells.
  observed_values = Hash.new{ |h, k| h[k] = [] }
  cells.each do |cell|
    codes.each do |code|
      observed_values[code] << cell.get_arg(code)
    end
  end

  observed_values.each_value{ |v| v.uniq! }

  # Init contingency tables for each code name
  tables = Hash.new
  observed_values.each_pair do |codename, codevalues|
    tables[codename] = CTable.new(*codevalues)
  end

  # Get the pairs of corresponding primary and reliability cells
  cellPairs = Hash.new
  rel_col.cells.each do |relcell|
    cellPairs[relcell] = pri_col.cells.find{ |pricell| pricell.onset == relcell.onset} # match by onset times
  end

  cellPairs.each_pair do |pricell, relcell|
    codes.each do |x|
      tables[x].add(pricell.get_arg(x), relcell.get_arg(x))
    end
  end


  kappas = Hash.new
  tables.each_pair do |codename, ctable|
    kappas[codename] = ctable.kappa
  end

  return kappas, tables
end
alias :computeKappa :compute_kappa

# Construct a Ruby representation of the Datavyu column, if it exists.
# @param name [String] the name of the column in the spreadsheet
# @return [RColumn] Ruby object representation of the variable inside Datavyu or nil if the named column does not exist
# @note Prints warning message to console if column name is not found in spreadsheet.
# @example
#       trial = get_column("trial")
def get_column(name)

  var = $db.getVariable(name)
  if (var == nil)
    printNoColumnFoundWarning(name.to_s)
    return nil
  end

  # Convert each cell into an array and store in an array of arrays
  cells = var.getCells()
  arg_names = Array.new

  # Now get the arguments for each of the cells

  # For matrix vars only
  type = var.getRootNode.type
  if type == Argument::Type::MATRIX
    # Matrix var
    arg_names = Array.new
    for arg in var.getRootNode.childArguments
      arg_names << arg.name
    end
  else
    # Nominal or text
    arg_names = ["var"]
  end

  v = RColumn.new
  v.name = name
  v.old_args = arg_names
  v.type = type
  v.set_cells(cells, arg_names)
  v.sort_cells
  v.dirty = false
  v.db_var = var

  return v
end
alias :getVariable :get_column
alias :getColumn :get_column


# Translate a Ruby column object into a Datavyu column and saves it to the spreadsheet.
# If two parameters are specified, the first parameter is the name under which the column will be saved.
# @note This function will overwrite existing spreadsheet columns with the same name as specified column / name.
# @param args [String, RColumn] the name and RColumn object to save; the name parameter may be omitted
# @return nil
# @example
#       trial = get_column("trial")
#         ... Do some modification to trial ...
#       set_column(trial)
def set_column(*args)

  if args.length == 1
    var = args[0]
    name = var.name
  elsif args.length == 2
    var = args[1]
    name = args[0]
  end

  # If substantial changes have been made to the structure of the column,
  # just delete the whole thing first.
  # If the column was dirty, redo the vocab too
  if var.db_var == nil or var.db_var.get_name != name

    if getColumnList().include?(name)
      deleteVariable(name)
    end
    # Create a new variable
    v = $db.createVariable(name, Argument::Type::MATRIX)
    var.db_var = v

    if var.arglist.length > 0
      var.db_var.removeArgument("code01")
    end

    # Set variable's vocab
    for arg in var.arglist
      new_arg = v.addArgument(Argument::Type::NOMINAL)
      new_arg.name = arg
      main_arg = var.db_var.getRootNode()
      child_args = main_arg.childArguments

      child_args.get(child_args.length-1).name = arg

      var.db_var.setRootNode(main_arg)
    end
    var.db_var = v
  end

  #p var
  if var.dirty
    # deleteVariable(name)
    # If the variable is dirty, then we have to do something to the vocab.
    # Compare the variable's vocab and the Ruby cell version to see
    # what is different.

    #p var.db_var
    if var.db_var.getRootNode.type == Argument::Type::MATRIX
      values = var.db_var.getRootNode.childArguments
      #p values
      for arg in var.old_args
        #p var.old_args
        flag = false
        for dbarg in values
          if arg == dbarg.name
            flag = true
            break
          end
        end
        # If we didn't find it in dbarg, we have to create it
        if flag == false
          # Add the argument
          new_arg = var.db_var.addArgument(Argument::Type::NOMINAL)

          # Make sure argument doesn't have < or > in it.
          arg = arg.delete("<").delete(">")
          # Change the argument's name by getting the variable back,
          # and then setting it. This hoop jumping is annoying.
          new_arg.name = arg
          main_arg = var.db_var.getRootNode()
          child_args = main_arg.childArguments

          child_args.get(child_args.length-1).name = arg

          var.db_var.setVariableType(main_arg)
        end
      end

      # Now see if we have deleted any arguments
      deleted_args = values.map { |x| x.name } - var.old_args
      deleted_args.each do |arg|
        puts "DELETING ARG: #{arg}"
        var.db_var.removeArgument(arg)
      end
    end


  end

  # Create new cells and fill them in for each cell in the variable
  for cell in var.cells
    # Copy the information from the ruby variable to the new cell

    if cell.db_cell == nil or cell.parent != name
      cell.db_cell = var.db_var.createCell()
    end

    value = cell.db_cell.getCellValue()

    if cell.onset != cell.db_cell.getOnset
      cell.db_cell.setOnset(cell.onset)
    end

    if cell.offset != cell.db_cell.getOffset
      cell.db_cell.setOffset(cell.offset)
    end

    # Matrix cell
    if cell.db_cell.getVariable.getRootNode.type == Argument::Type::MATRIX
      values = cell.db_cell.getCellValue().getArguments()
      for arg in var.old_args
        # Find the arg in the dataStore's arglist that we are looking for
        for i in 0...values.size
          dbarg = values[i]
          dbarg_name = dbarg.getArgument.name
          if dbarg_name == arg and not ["", nil].include?(cell.get_arg(var.convert_argname(arg)))
            dbarg.set(cell.get_arg(var.convert_argname(arg)))
            break
          end
        end
      end

      # Non-matrix cell
    else
      value = cell.db_cell.getCellValue()
      value.set(cell.get_arg("var"))
    end

    # Save the changes back to the DB

  end
  # if var.hidden
  var.db_var.setHidden(var.hidden)
  # end
end
alias :setVariable :set_column
alias :setColumn :set_column


# Deletes a variable from the spreadsheet and rebuilds it from
# the given RColumn object.
# Behaves similar to setVariable(), but this will ALWAYS delete
# and rebuild the spreadsheet colum and its vocab.
def set_column!(*args)
  if args.length == 1
    var = args[0]
    name = var.name
  elsif args.length == 2
    var = args[1]
    name = args[0]
  end

  if getColumnList().include?(name)
    deleteVariable(name)
  end

  # Create a new variable
  v = $db.createVariable(name, Argument::Type::MATRIX)
  var.db_var = v

  if var.arglist.length > 0
    var.db_var.removeArgument("code01")
  end

  # Set variable's vocab
  for arg in var.arglist
    new_arg = v.addArgument(Argument::Type::NOMINAL)
    new_arg.name = arg
    main_arg = var.db_var.getRootNode()
    child_args = main_arg.childArguments

    child_args.get(child_args.length-1).name = arg

    var.db_var.setRootNode(main_arg)
  end
  var.db_var = v

  # Create new cells and fill them in for each cell in the variable
  for cell in var.cells
    # Copy the information from the ruby variable to the new cell
    cell.db_cell = var.db_var.createCell()

    value = cell.db_cell.getCellValue()

    if cell.onset != cell.db_cell.getOnset
      cell.db_cell.setOnset(cell.onset)
    end

    if cell.offset != cell.db_cell.getOffset
      cell.db_cell.setOffset(cell.offset)
    end

    # Matrix cell
    if cell.db_cell.getVariable.getRootNode.type == Argument::Type::MATRIX
      values = cell.db_cell.getCellValue().getArguments()
      for arg in var.old_args
        # Find the arg in the dataStore's arglist that we are looking for
        for i in 0...values.size
          dbarg = values[i]
          dbarg_name = dbarg.getArgument.name
          if dbarg_name == arg and not ["", nil].include?(cell.get_arg(var.convert_argname(arg)))
            dbarg.set(cell.get_arg(var.convert_argname(arg)))
            break
          end
        end
      end
      # Non-matrix cell
    else
      value = cell.db_cell.getCellValue()
      value.set(cell.get_arg("var"))
    end
  end

  # if var.hidden
  var.db_var.setHidden(var.hidden)
  # end
end
alias :setVariable! :set_column!

# Create a reliability column that is a copy
# of another column in the database, copying every nth cell and
# carrying over some of the arguments from the original, if wanted.
# @param relname [String, RColumn] the name of the reliability column to be created
# @param var_to_copy [String] the name of the variable in the database you wish to copy
# @param multiple_to_keep [Integer] the number of cells to skip.  For every other cell, use 2
# @param args_to_keep [Array<String>]: names of codes to keep from original column
# @return [RColumn] Ruby object representation of the rel column
# @example
#       rel_trial = make_rel("rel.trial", "trial", 2, "onset", "trialnum", "unit")
def make_reliability(relname, var_to_copy, multiple_to_keep, *args_to_keep)
  # Get the primary variable from the DB

  if var_to_copy.class == String
    var_to_copy = get_column(var_to_copy)
  else
    var_to_copy = get_column(var_to_copy.name)
  end

  if args_to_keep[0].class == Array
    args_to_keep = args_to_keep[0]
  end

  # Clip down cells to fit multiple to keep
  for i in 0..var_to_copy.cells.length-1
    if multiple_to_keep == 0
      var_to_copy.cells[i] = nil
    elsif var_to_copy.cells[i].ordinal % multiple_to_keep != 0
      var_to_copy.cells[i] = nil
    else
      var_to_copy.cells[i].ordinal = var_to_copy.cells[i].ordinal / multiple_to_keep
    end
  end
  # Clear out the nil cells
  var_to_copy.cells.compact!

  var_to_copy.cells.each do |cell|
    if !args_to_keep.include?("onset")
      cell.onset = 0
    end
    if !args_to_keep.include?("offset")
      cell.offset = 0
    end
    cell.arglist.each do |arg|
      if !args_to_keep.include?(arg)
        cell.change_arg(arg, "")
      end
    end
  end
  setVariable(relname, var_to_copy)
  return var_to_copy
end
alias :makeReliability :make_reliability
alias :make_rel :make_reliability

# Create blank column.
# @note Column does not exist in Datavyu spreadsheet unless saved with #set_column.
# @param name [String] name of the column
# @param args [Array<String>] list of codes to add to column; must specify at least one code name
# @return [RColumn] Ruby column object
# @note Code names should be all lower-case and contain no special characters other than underscores.
# @example
#   trial = new_column("trial", "trialnum", "unit")
#   blank_cell = trial.new_cell()
#   set_column(trial)
def new_column(name, *args)
  print_debug "Creating new variable"

  # Use default code when no codes are specified.
  args = ['code01'] if args.empty?

  v = RColumn.new

  v.name = name

  v.dirty = true

  print_debug args[0].class
  print_debug args
  if args[0].class == Array
    args = args[0]
  end
  print_debug args

  # Set the argument names in arg_names and set the database internal style with <argname> in old_args
  arg_names = Array.new
  old_args = Array.new
  for arg in args
    print_debug arg
    arg_names << arg
    old_args << arg.to_s
  end
  c = Array.new
  v.old_args = old_args
  v.set_cells(nil, arg_names)

  # Return reference to this variable for the user
  print_debug "Finished creating variable"
  return v
end
alias :create_new_column :new_column
alias :createNewColumn :new_column
alias :createVariable :new_column
alias :createNewVariable :new_column
alias :create_column :new_column
alias :createColumn :new_column


# Makes a duration based reliability column
# based on John's method.  It will create two new columns, one
# that contains a cell with a number for that block, and another
# blank column for the free coding within that block.
# @param relname [String] name of the rel column to be made
# @param var_to_copy [String] name of column being copied
# @param binding_column [String] name of column to bind copy to
# @param block_dur [Integer] duration, in seconds, for each block
# @param skip_blocks [Integer] multiple of block_dur to skip between coding blocks
# @return nil
# @note Column is written to spreadsheet.
# @deprecated
def make_duration_block_rel(relname, var_to_copy, binding_column, block_dur, skip_blocks)
  block_var = new_column(relname + "_blocks", "block_num")
  rel_var = make_rel(relname, var_to_copy, 0)

  var_to_copy = get_column(var_to_copy)
  binding_col = get_column(binding_column)


  block_dur = block_dur * 1000 # Convert to milliseconds
  block_num = 1
  for bindcell in binding_col.cells
    cell_dur = bindcell.offset - bindcell.onset
    if cell_dur <= block_dur
      cell = block_var.new_cell()
      cell.change_code("block_num", block_num.to_s)
      cell.onset = bindcell.onset
      cell.offset = bindcell.offset
      block_num += 1
    else
      num_possible_blocks = cell_dur / block_dur #Integer division
      if num_possible_blocks > 0
        for i in 0..num_possible_blocks
          if i % skip_blocks == 0
            cell = block_var.new_cell()
            cell.change_code("block_num", block_num.to_s)
            cell.onset = bindcell.onset + i * block_dur
            if bindcell.onset + (i + 1) * block_dur <= bindcell.offset
              cell.offset =  bindcell.onset + (i + 1) * block_dur
            else
              cell.offset = bindcell.offset
            end
            block_num += 1
          end
        end
      end
    end
  end
  set_column(relname + "_blocks", block_var)
end
alias :makeDurationBlockRel :make_duration_block_rel

# Add a new code to a column
# @param var [String, RColumn] The variable to add args to.  This can be a name or a variable object.
# @param args [Array<String>] A list of the arguments to add to var (can be any number of args)
#
# @return [RColumn] the new Ruby representation of the variable.  Write it back to the database to save it.
#
# @example
#   test = add_codes_to_column("test", "arg1", "arg2", "arg3")
#   set_column("test", test)
def add_codes_to_column(var, *args)
  if var.class == "".class
    var = get_column(var)
  end

  var_new = createVariable(var.name, var.arglist + args)

  for cell in var.cells
    new_cell = var_new.make_new_cell()
    new_cell.change_arg("onset", cell.onset)
    new_cell.change_arg("offset", cell.offset)
    for arg in var.arglist
      v = eval "cell.#{arg}"
      new_cell.change_arg(arg, v)
    end
  end

  return var_new
end
alias :add_args_to_var :add_codes_to_column
alias :addCodesToColumn :add_codes_to_column
alias :addArgsToVar :add_codes_to_column


# @!visibility private
# Helper method for #create_mutually_exclusive
def scan_for_bad_cells(col)
  error = false
  for cell in col.cells
    if cell.onset > cell.offset
      puts "ERROR AT CELL " + cell.ordinal.to_s + " IN COLUMN " + col.name + ", the onset is > than the offset."
      error = true
    end
    if error
      puts "Please fix these errors, as the script cannot continue until then."
      exit
    end
  end
end

# @!visibility private
# Helper method for #create_mutually_exclusive
def get_later_overlapping_cell(col)
  col.sort_cells()
  overlapping_cells = Array.new
  for i in 0..col.cells.length - 2
    cell1 = col.cells[i]
    cell2 = col.cells[i+1]
    if (cell1.onset <= cell2.onset and cell1.offset >= cell2.onset)
      overlapping_cells << cell2
    end
  end
  return overlapping_cells
end

# @!visibility private
# Helper method for #create_mutually_exclusive
def fix_one_off_cells(col1, col2)
  for i in 0..col1.cells.length-2
    cell1 = col1.cells[i]
    for j in 0..col2.cells.length-2
      cell2 = col2.cells[j]

      if (cell1.onset - cell2.onset).abs == 1
        print_debug "UPDATING CELL"
        cell2.change_arg("onset", cell1.onset)
        print_debug "CELL2 ONSET IS NOW " + cell1.onset.to_s
        if j > 0 and col2.cells[j-1].offset == cell2.offset
          col2.cells[j-1].change_arg("offset", col2.cells[i-1].offset + 1)
        end
      end

      if (cell1.offset - cell2.offset).abs == 1
        print_debug "UPDATING CELL"
        cell2.change_arg("offset", cell1.offset)
        print_debug "CELL2 OFFSET IS NOW " + cell1.offset.to_s
        if col2.cells[j+1].onset == cell2.offset
          col2.cells[j+1].change_arg("onset", col2.cells[i-1].onset + 1)
        end
      end

      if cell2.onset - cell1.offset == 1
        print_debug "UPDATING CELL"
        cell1.change_arg("offset", cell2.onset)
        print_debug "CELL1 OFFSET IS NOW " + cell2.onset.to_s
        if col1.cells[i+1].onset == cell1.offset
          col1.cells[i+1].change_arg("onset", col1.cells[i+1].onset + 1)
        end
      end
      if cell1.onset - cell2.offset == 1
        print_debug "UPDATING CELL"
        cell2.change_arg("offset", cell1.onset)
        print_debug "CELL2 OFFSET IS NOW " + cell1.onset.to_s
        if col2.cells[j+1].onset == cell2.offset
          col2.cells[j+1].change_arg("onset", col2.cells[i+1].onset + 1)
        end
      end
    end
  end
end

# Combine two columns into a third column.
# The new column's code list is a union of the original two columns with a prefix added to each code name.
# The default prefix is the name of the source column (e.g. column "task" code "ordinal" becomes "task_ordinal")
# Create a new column from two others, mixing their cells together
# such that the new variable has all of the arguments of both other variables
# and a new cell for each overlap and mixture of the two cells.
# @param name name of the new variable.
# @param var1name name of the first variable to be mutexed.
# @param var2name name of the second variable to be mutexed.
# @param var1_argprefix [String] (optional) String to prepend to codes of column 1; defaults to name of column 1
# @param var2_argprefix [String] (optional) String to prepend to codes of column 2; defaults to name of column 2
# @return [RColumn] The new Ruby representation of the variable.  Write it back to the database to save it.
#
# @example
#   test = create_mutually_exclusive("test", "var1", "var2")
#   set_column("test",test)
def create_mutually_exclusive(name, var1name, var2name, var1_argprefix=nil, var2_argprefix=nil)
  if var1name.class == "".class
    var1 = get_column(var1name)
  else
    var1 = var1name
  end
  if var2name.class == "".class
    var2 = get_column(var2name)
  else
    var2 = var2name
  end

  scan_for_bad_cells(var1)
  scan_for_bad_cells(var2)

  for cell in var1.cells
    if cell.offset == 0
      puts "ERROR: CELL IN " + var1.name + " ORD: " + cell.ordinal.to_s + "HAS BLANK OFFSET, EXITING"
      exit
    end
  end

  for cell in var2.cells
    if cell.offset == 0
      puts "ERROR: CELL IN " + var2.name + " ORD: " + cell.ordinal.to_s + "HAS BLANK OFFSET, EXITING"
      exit
    end
  end

  # TODO Handle special cases where one or both of columns have no cells

  # TODO Handle special case where column has a cell with negative time

  # Get the earliest time between the two cols
  time1_on = 9999999999
  time2_on = 9999999999

  time1_off = 0
  time2_off = 0
  if var1.cells.length > 0
    time1_on = var1.cells[0].onset
    time1_off = var1.cells[var1.cells.length-1].offset
  end
  if var2.cells.length > 0
    time2_on = var2.cells[0].onset
    time2_off = var2.cells[var2.cells.length-1].offset
  end
  start_time = [time1_on, time2_on].min

  # And the end time
  end_time = [time1_off, time2_off].max


  # Create the new variable
  if var1_argprefix == nil
    var1_argprefix = var1.name.gsub(/(\W)+/, "").downcase + "___"
    var1_argprefix.gsub(".", "")
  end
  if var2_argprefix == nil
    var2_argprefix = var2.name.gsub(/(\W)+/, "").downcase + "___"
    var2_argprefix.gsub(".", "")
  end

  v1arglist = var1.arglist.map { |arg| var1_argprefix + arg }
  v2arglist = var2.arglist.map { |arg| var2_argprefix + arg }

  # puts "NEW ARGUMENT NAMES:", v1arglist, v2arglist
  args = Array.new
  args << (var1_argprefix + "ordinal")
  args += v1arglist

  args << (var2_argprefix + "ordinal")
  args += v2arglist

  # puts "Creating mutex var", var1.arglist
  mutex = createVariable(name, args)
  # puts "Mutex var created"

  # And finally begin creating new cells
  v1cell = nil
  v2cell = nil
  next_v1cell_ind = nil
  next_v2cell_ind = nil

  time = start_time
  # puts "Start time", start_time
  # puts "End time", end_time

  flag = false

  count = 0

  #######################
  # BEGIN NEW MUTEX
  # Idea here: gather all of the time changes.
  # For each time change get the corresponding cells involved in that change.
  # Create the necessary cell at each time change.
  #######################

  time_changes = Set.new
  v1_cells_at_time = Hash.new
  v2_cells_at_time = Hash.new


  # Preprocess relevant cells and times
  for cell in var1.cells + var2.cells
    time_changes.add(cell.onset)
    time_changes.add(cell.offset)
  end


  time_changes = time_changes.to_a.sort
  if $debug
    p time_changes
  end
  # p time_changes


  mutex_cell = nil
  mutex_cell_parent = nil

  # TODO: make these handle empty cols
  v1cell = var1.cells[0]
  prev_v1cell = nil
  prev_v2cell = nil
  v2cell = var2.cells[0]
  v1idx = 0
  v2idx = 0

  #
  for i in 0..time_changes.length-2
    t0 = time_changes[i]
    t1 = time_changes[i+1]

    # Find the cells that are active during these times
    for j in v1idx..var1.cells.length-1
      c = var1.cells[j]
      v1cell = nil
      if $debug
        p "---", "T1", t0, t1, c.onset, c.offset, "---"
      end
      if c.onset <= t0 and c.offset >= t1 and (t1-t0 > 1 or (c.onset==t0 and c.offset==t1))
        v1cell = c
        v1idx = j
        # p t0, t1, "Found V1"
        break
        # elsif c.onset > t1
        #   break
      else
        v1cell = nil
      end
    end

    for j in v2idx..var2.cells.length-1
      c = var2.cells[j]
      v2cell = nil
      # p "---", "T2", t0, t1, c.onset, c.offset, "---"
      if c.onset <= t0 and c.offset >= t1 and (t1-t0 > 1 or (c.onset==t0 and c.offset==t1))
        v2cell = c
        v2idx = j
        # p t0, t1, "Found V2"
        break
        # elsif c.onset > t1
        #   break
      else
        v2cell = nil
      end
    end

    if v1cell != nil or v2cell != nil
      mutex_cell = mutex.create_cell

      mutex_cell.change_arg("onset", t0)
      mutex_cell.change_arg("offset", t1)
      fillMutexCell(v1cell, v2cell, mutex_cell, mutex, var1_argprefix, var2_argprefix)
    end

  end


  # Now that we have all of the necessary temporal information
  # go through each time in the list and create a cell

  for arg in mutex.arglist
    mutex.change_arg_name(arg, arg.gsub("___", "_"))
  end
  for i in 0..mutex.cells.length-1
    c = mutex.cells[i]
    c.change_arg("ordinal", i+1)
  end
  puts "Created a column with #{mutex.cells.length} cells."

  return mutex
end
alias :createMutuallyExclusive :create_mutually_exclusive

# @!visibility private
# Helper method for #create_mutually_exclusive
def fill_mutex_cell(v1cell, v2cell, cell, mutex, var1_argprefix, var2_argprefix)
  if v1cell != nil and v2cell != nil
    for arg in mutex.arglist
      a = arg.gsub(var1_argprefix, "")
      if arg.index(var1_argprefix) == 0
        v = eval "v1cell.#{a}"
        cell.change_arg(arg, v)
      end

      a = arg.gsub(var2_argprefix, "")
      if arg.index(var2_argprefix) == 0
        v = eval "v2cell.#{a}"
        cell.change_arg(arg, v)
      end
    end

  elsif v1cell != nil and v2cell == nil
    for arg in mutex.arglist
      a = arg.gsub(var1_argprefix, "")
      if arg.index(var1_argprefix) == 0
        v = eval "v1cell.#{a}"
        cell.change_arg(arg, v)
      end
    end

  elsif v1cell == nil and v2cell != nil
    for arg in mutex.arglist
      a = arg.gsub(var2_argprefix, "")
      if arg.index(var2_argprefix) == 0
        v = eval "v2cell.#{a}"
        cell.change_arg(arg, v)
      end
    end
  end
end
alias :fillMutexCell :fill_mutex_cell

# Combine cells of different columns into a new column.
# Iteratively runs #create_mutually_exclusive on additional columns.
# @note Not thoroughly tested. Only merges first and last columns.
# @todo verify this works
def combine_columns(name, varnames)
  stationary_var = varnames[0]
  for i in 1...varnames.length
    next_var = varnames[i]
    var = create_mutually_exclusive(name, stationary_var, next_var)
  end
  return var
end

# Combine cells of different columns into a new column.
# Intended to mimic functionality of create_mutually_exclusive() and combine_columns
# but not guaranteed to produce equivalent results.
# @param name [String] name of result column
# @return [RColumn] merged column
def merge_columns(name, *cols)
  # Handle degenerate cases
  return nil if cols.nil? || cols.size == 0

  # Ensure cols contains RColumns
  cols.map! do |x|
    case x
    when String
      get_column(x)
    when RColumn
      x
    else
      raise "Unhandled column value or class: #{x}, #{x.class}."
    end
  end

  # Do nothing if only one column given.
  return cols.first if cols.size == 1

	# Concatenate arglists and cells.
  # Codes have their column name and an underscore prepended to them.
	my_args = []
	all_cells = []
	cols.each{
		|x|
		my_args << x.name.downcase+"_ordinal"
		my_args << x.arglist.map{ |y| x.name.downcase+"_"+y}
		all_cells << x.cells
	}
	my_args.flatten!
	all_cells.flatten!

	ncol = new_column(name, *my_args)

  # Convert point cells to have offsets = onset + 1
  all_cells.each{ |x| x.offset = x.onset + 1 if x.onset == x.offset }

	# Gather onsets and offsets and collect unique times into a single array
	onsets = all_cells.map(&:onset)
	offsets = all_cells.map(&:offset)
	times = (onsets+offsets).uniq.sort

	# For each consecutive time in times, create a new cell over that interval.
	if times.size>0
		onset = times.first
		for offset in times[1..-1]
			ncell = ncol.make_new_cell()
			ncell.onset = onset
			ncell.offset = offset

      # Find an enclosing cell (if any) from each of the columns for this time region
			ocells = cols.map do |col|
				col.cells.find{ |y| y.contains(ncell) }
			end

			for c in ocells
				unless c.nil?
					ncell.change_arg(c.parent.downcase+"_ordinal", c.ordinal)
					c.arglist.each do |a|
						ncell.change_arg(c.parent.downcase+"_"+a, c.get_code(a))
					end
				end
			end
			onset = offset
		end
	end

	return ncol
end

# Loads a new database from a file.
# @note DOES NOT ALTER THE GUI.
# @note Use #File.expand_path and related methods to convert from relative to absolute path.
# @param filename The FULL PATH to the saved Datavyu file.
# @return [Array] An array containing two items: dataStore, the spreadsheet data, and pj the project data. Set dataStore and pj to $db and $pj, respectively (see example)
# @example
#   $db,$pj = load_db("/Users/username/Desktop/test.opf")
def load_db(filename)
  # Raise file not found error unless file exists
  unless File.exist?(filename)
    raise "File does not exist. Please make sure to put the full path to the file."
  end

  infile = java.io.File.new(filename)

  print_debug "Opening Project: #{infile}"

  # Create the controller that holds all the logic for opening projects and
  # databases.
  open_c = OpenController.new

  # Opens a project and associated database (i.e. either compressed or
  # uncompressed .shapa files). If you want to just open a standalone database
  # (i.e .odb or .csv file) call open_c.open_database("filename") instead. These
  # methods do *NOT* open the project within the Datavyu UI.
  db = nil
  proj = nil
  if filename.include?(".csv")
    db = open_c.open_datastore(filename)
  else
    db = open_c.open_project(infile)
    # Get the project that was opened (if you want).
    proj = open_c.get_project
  end

  # Get the database that was opened.
  db = open_c.get_data_store

  # If the open went well - query the database, do calculations or whatever
  unless db.nil?
    # This just prints the number of columns in the database.
    print_debug "SUCCESSFULLY Opened a project with #{db.get_all_variables.length.to_s} columns!"
  else
    raise "Unable to open file '#{filename}'"
  end

  print_debug "#{filename} has been loaded."

  return db, proj
end
alias :loadDB :load_db

# Saves the current $db and $pj variables to filename.  If
# filename ends with .csv, it saves a .csv file.  Otherwise it saves
# it as a .opf.
# @note Use #File.expand_path and related methods to convert from relative to absolute path.
# @param filename [String] The FULL PATH to where the Datavyu file should be saved.
# @return nil
# @example
#   save_db("/Users/username/Desktop/test.opf")
def save_db(filename)
  print_debug "Saving Database: " + filename

  filename = File.expand_path(filename)

  # Create the controller that holds all the logic for opening projects and
  # databases.
  save_c = SaveController.new

  #
  # Saves a database (i.e. a .odb or .csv file). If you want to save a project
  # call save_project("project file", project, database) instead.
  # These methods do *NOT* alter the Datavyu UI.
  #
  if filename.include?('.csv')
    save_c.save_database(filename, $db)
  else
    if $pj == nil or $pj.getDatabaseFileName == nil
        $pj = Project.new()
        $pj.setDatabaseFileName("dataStore")
        dbname = filename[filename.rindex("/")+1..filename.length]
        $pj.setProjectName(dbname)
    end
    save_file = java.io.File.new(filename)
    save_c.save_project(save_file, $pj, $db)
  end

  print_debug "Save successful."
end
alias :saveDB :save_db

# Deletes a column from the spreadsheet.
# @note This change is immediately reflected in the spreadsheet and is irreversible.
# @param colname [RColumn, String] column to delete
# @return nil
def delete_column(colname)
  if colname.class != "".class
    colname = colname.name
  end
  col = $db.getVariable(colname)
  if (col == nil)
    printNoColumnFoundWarning(colname.to_s)
  end
  $db.removeVariable(col)
end
alias :deleteColumn :delete_column
alias :delete_variable :delete_column
alias :deleteVariable :delete_column

# Let the user know that a given column was not found. Error is confusing, this should clarify.
def print_no_column_found_warning(colName)
  puts "WARNING: No column with name '" + colName + "' was found!"
end
alias :printNoColumnFoundWarning :print_no_column_found_warning

# Opens an old, closed database format MacSHAPA file and loads it into the current open database.
# NOTE This will only read in matrix and string variables.  Predicates are not yet supported. Queries will not be read in.  Times are translated to milliseconds for compatibility with Datavyu.
# @param filename [String] The FULL PATH to the saved MacSHAPA file.
# @param write_to_gui [true, false] Whether the MacSHAPA file should be read into the database currently open in the GUI or whether it should just be read into the Ruby interface.  After this script is run $db and $pj are now the MacSHAPA file.
# @return [Array] An array containing two items: the spreadsheet data and the project information. Set to $db and $pj, respectively (see example).
# @todo fix linter warnings
# @example
#   $db,$pj = load_db("/Users/username/Desktop/test.opf")
def load_macshapa_db(filename, write_to_gui, *ignore_vars)

  # Create a new DB for us to use so we don't touch the GUI... some of these
  # files can be huge.
  # Since I don't know how to make a whole new project, lets just load a blank file.
  # TODO why is this section commented out??
  if not write_to_gui
    #$db,$pj = load_db("/Users/j4lingeman/Desktop/blank.opf")
    # $db = Datastore.new
    # $pj = Project.new()
  end


  puts "Opening file"
  f = File.open(filename, 'r')

  puts "Opened file"
  # Read and split file by lines.  '\r' is used because that is the default
  # format for OS9 files.
  lines = ""
  while (line = f.gets)
    lines += line
  end
  lines = lines.split(/[\r\n]/)

  # Find the variable names in the file and use these to create and set up
  # our columns.
  predIndex = lines.index("***Predicates***")
  varIndex = lines.index("***Variables***")
  spreadIndex = lines.index("***SpreadPane***")
  predIndex += 2

  variables = Hash.new
  varIdent = Array.new

  while predIndex < varIndex
    l = lines[predIndex].split(/ /)[5]
    varname = l[0..l.index("(") - 1]
    if varname != "###QueryVar###" and varname != "div" and varname != "qnotes" \
			and not ignore_vars.include?(varname)
      print_debug varname

      # Replace non-alphabet with underscores
      vname2 = varname.gsub(/\W+/, '_')
      if vname2 != varname
        puts "Replacing #{varname} with #{vname2}"
        varname = vname2
      end

      variables[varname] = l[l.index("(")+1..l.length-2].split(/,/)
      varIdent << l
    end
    predIndex += 1
  end

  puts "Got predicate index"

  # Create the columns for the variables
  variables.each do |key, value|
    # Create column
    if getColumnList().include?(key)
      deleteVariable(key)
    end


    args = Array.new
    value.each { |v|
      # Strip out the ordinal, onset, and offset.  These will be handled on a
      # cell by cell basis.
      if v != "<ord>" and v != "<onset>" and v != "<offset>"
        v1 = v.gsub(/\<|\>/, '').gsub('#', 'number').gsub('&', 'and')
        v2 = RColumn.sanitize_codename(v1)
        puts "Changing code #{v1} in column #{key} to: #{v2}" if(v2 != v1)
        # args << v.sub("<", "").sub(">", "")
        args << v2
      end
    }

    setVariable(createVariable(key, args))
  end

  # Search for where in the file the var's cells are, create them, then move
  # on to the next variable.
  varSection = lines[varIndex..spreadIndex]

  varIdent.each do |id|

    # Search the variable section for the above id
    varSection.each do |l|
      line = l.split(/[\t\s]/) # @todo linter error : dup char class
      if line[2] == id

        print_debug id
        varname = id.slice(0, id.index("(")).gsub(/\W+/,'_')
        if get_column_list.include?(varname)
          col = get_column(varname)
        else
          puts "Column #{varname} not found. Skipping."
          next
        end

        #print_debug varname
        start = varSection.index(l) + 1

        stringCol = false

        if varSection[start - 2].index("strID") != nil
          stringCol = true
        end

        #Found it!  Now build the cells
        while varSection[start] != "0"

          if stringCol == false
            cellData = varSection[start].split(/[\t]/)
            cellData[cellData.length - 1] = cellData[cellData.length-1][cellData[cellData.length-1].index("(")..cellData[cellData.length-1].length]
          else
            cellData = varSection[start].split(/[\t]/)
          end

          # Init cell to null

          cell = col.create_cell

          # Convert onset/offset from 60 ticks/sec to milliseconds
          onset = cellData[0].to_i / 60.0 * 1000
          offset = cellData[1].to_i / 60.0 * 1000

          # Set onset/offset of cell
          cell.change_arg("onset", onset.round)
          cell.change_arg("offset", offset.round)

          # Split up cell data
          data = cellData[cellData.length - 1]
          print_debug data
          if stringCol == false
            data = data[1..data.length-2]
            data = data.gsub(/[() ]*/, "")
            data = data.split(/,/)
          elsif data != nil #Then this is a string var
            data = data.strip()
            if data.split(" ").length > 1
              data = data[data.index(" ")..data.length] # Remove the char count
              data = data.gsub("/", " or ")
              data = data.gsub(/[^\w ]*/, "")
              data = data.gsub(/  /, " ")
            else
              data = ""
            end
          else
            data = Array.new
            data << nil
          end
          # Cycle thru cell data arguments and fill them into the cell matrix
          narg = 0
          if data.is_a?(String)
            argname = cell.arglist.last
            cell.change_arg(argname, data)
          elsif data.is_a?(Array)
            data.each_with_index do |d, i|
              print_debug cell.arglist[1]
              argname = cell.arglist[i]
              if d == nil
                cell.change_arg(argname, "")
              elsif d == "" or d.index("<") != nil
                cell.change_arg(argname, "")
              else
                cell.change_arg(argname, d)
              end
            end
          end
          start += 1
        end
        setVariable(col)
      end
    end
  end

  f.close()

  return $db, $pj
end
alias :loadMacshapaDB :load_macshapa_db

# Updated function. Keeping original as is in case edits break previously working imports.
def load_macshapa_db2(filename, write_to_gui, *ignore_vars)
  # Create a new DB for us to use so we don't touch the GUI... some of these
  # files can be huge.
  # Since I don't know how to make a whole new project, lets just load a blank file.
  if not write_to_gui
    #$db,$pj = load_db("/Users/j4lingeman/Desktop/blank.opf")
    # $db = Datastore.new
    # $pj = Project.new()
  end


  puts "Opening file"
  f = File.open(filename, 'r')

  puts "Opened file"
  # Read and split file by lines.  '\r' is used because that is the default
  # format for OS9 files.
  lines = ""
  while (line = f.gets)
    lines += line
  end
  lines = lines.split(/[\r\n]/)

  # Find the variable names in the file and use these to create and set up
  # our columns.
  predIndex = lines.index("***Predicates***")
  varIndex = lines.index("***Variables***")
  spreadIndex = lines.index("***SpreadPane***")
  predIndex += 2

  variables = Hash.new
  varIdent = Array.new

  while predIndex < varIndex
    line = lines[predIndex].strip
    parser = /(?<d1>\d+)\s*(?<d2>\d+)\s*(?<d3>\d+)\s*(?<vardata>(?<varname>.*)\((?<codes>.*)\))/
    matches = parser.match(line)
    # l = lines[predIndex].split(/ /)[5]
    # varname = l[0..l.index("(") - 1]
    varname = matches['varname']
    if varname != "###QueryVar###" and varname != "div" and varname != "qnotes" \
			and not ignore_vars.include?(varname)
      print_debug varname

      # Replace non-alphabet with underscores
      vname2 = varname.gsub(/\W+/, '_')
      if vname2 != varname
        puts "Replacing #{varname} with #{vname2}"
        varname = vname2
      end

      variables[varname] = matches['codes'].split(/,/)
      varIdent << matches['vardata']
    end
    predIndex += 1
  end

  puts "Got predicate index"

  # Create the columns for the variables
  variables.each do |key, value|
    puts "Creating column: #{key}"
    # Create column
    args = Array.new
    value.each { |v|
      # Strip out the ordinal, onset, and offset.  These will be handled on a
      # cell by cell basis.
      if v != "<ord>" and v != "<onset>" and v != "<offset>"
        args << v.sub("<", "").sub(">", "")
      end
    }

    set_column(new_column(key, args))
  end

  # Search for where in the file the var's cells are, create them, then move
  # on to the next variable.
  varSection = lines[varIndex..spreadIndex]

  varIdent.each do |id|

    # Search the variable section for the above id
    varSection.each do |l|
      line = l.split(/[\t\s]/)
      if line[2] == id

        print_debug id
        colname = id.slice(0, id.index("(")).gsub(/\W+/,'_')
        col = get_column(colname)
        #print_debug varname
        start = varSection.index(l) + 1

        stringCol = false

        if varSection[start - 2].index("strID") != nil
          stringCol = true
        end

        #Found it!  Now build the cells
        while varSection[start] != "0"

          if stringCol == false
            cellData = varSection[start].split(/[\t]/)
            cellData[cellData.length - 1] = cellData[cellData.length-1][cellData[cellData.length-1].index("(")..cellData[cellData.length-1].length]
          else
            cellData = varSection[start].split(/[\t]/)
          end

          # Init cell to null

          cell = col.create_cell

          # Convert onset/offset from 60 ticks/sec to milliseconds
          onset = cellData[0].to_i / 60.0 * 1000
          offset = cellData[1].to_i / 60.0 * 1000

          # Set onset/offset of cell
          cell.change_arg("onset", onset.round)
          cell.change_arg("offset", offset.round)

          # Split up cell data
          data = cellData[cellData.length - 1]
          print_debug data
          if stringCol == false
            data = data[1..data.length-2]
            data = data.gsub(/[() ]*/, "")
            data = data.split(/,/)
          elsif data != nil #Then this is a string var
            data = data.strip()
            if data.split(" ").length > 1
              data = data[data.index(" ")..data.length] # Remove the char count
              data = data.gsub("/", " or ")
              data = data.gsub(/[^\w ]*/, "")
              data = data.gsub(/  /, " ")
            else
              data = ""
            end
          else
            data = Array.new
            data << nil
          end
          # Cycle thru cell data arguments and fill them into the cell matrix
          narg = 0
          if data.is_a?(String)
            argname = cell.arglist.last
            cell.change_arg(argname, data)
          elsif data.is_a?(Array)
            data.each_with_index do |d, i|
              print_debug cell.arglist[1]
              argname = cell.arglist[i]
              if d == nil
                cell.change_arg(argname, "")
              elsif d == "" or d.index("<") != nil
                cell.change_arg(argname, "")
              else
                cell.change_arg(argname, d)
              end
            end
          end
          start += 1
        end
        setVariable(col)
      end
    end
  end

  f.close()

  return $db, $pj
end

# Transfers columns between databases.
# If db1 or db2 are set to the empty string "", then that database is the current database in $db (usually the GUI's database).
# So if you want to transfer a column into the GUI, set db2 to "".
# If you want to tranfer a column from the GUI into a file, set db1 to "".
# Setting remove to true will DELETE THE COLUMNS YOU ARE TRANSFERRING FROM DB1.  Be careful!
# @param db1 [String] The FULL PATH toa Datavyu file or "" to use the currently opened database. Columns are transferred FROM here.
# @param db2 [String]: The FULL PATH to the saved Datavyu file or "" to use the currently opened database.  Columns are tranferred TO here.
# @param remove [true, false] Set to true to delete columns in DB1 as they are moved to db2.  Set to false to leave them intact.
# @param varnames [Array<String>] column names (requires at least 1): You can specify as many column names as you like that will be retrieved from db1.
# @return nil
# @example
#  # Transfer column "idchange" from test.opf to the currently open spreadsheet in Datavyu. Do not delete "idchange" from test.opf.
#  transfer_columns("/Users/username/Desktop/test.opf", "", true, "idchange")
def transfer_columns(db1, db2, remove, *varnames)
  # Save the current $db and $pj global variables
  saved_db, saved_proj = $db, $pj

  # If varnames was specified as a hash, flatten it to an array
  varnames.flatten!

  # Display args when debugging
  print_debug("="*20)
  print_debug("#{__method__} called with following args:")
  print_debug(db1, db2, remove, varnames)
  print_debug("="*20)

  # Handle degenerate case of same source and destination
  if db1==db2
    puts "Warning: source and destination are identical.  No changes made."
    return nil
  end

  # Set the source database, loading from file if necessary.
  # Raises file not found error and returns nil if source database does not exist.
  db1path = ""
  begin
    if db1!=""
      db1path = File.expand_path(db1)
      if !File.readable?(db1path)
        raise "Error! File not readable : #{db1}"
      end
      print_debug("Loading source database from file : #{db1path}")
      from_db, from_proj = loadDB(db1path)
    else
      from_db, from_proj = $db, $pj
    end
  rescue StandardError => e
    puts e.message
    puts e.backtrace
    return nil
  end

  # Set the destination database, loading from file if necessary.
  # Raises file not found error and returns nil if destination database does not exist.
  db2path = ""
  begin
    if db2!=""
      db2path = File.expand_path(db2)
      if !File.writable?(db2path)
        raise "Error! File not writable : #{db2}"
      end
      print_debug("Loading destination database from file : #{db2path}")
      to_db, to_proj = loadDB(db2path)
      #$db,$pj = loadDB(db2path)
    else
      to_db, to_proj = $db, $pj
    end
  rescue StandardError => e
    puts e.message
    puts e.backtrace
    return nil
  end

  # Set working database to source database to prepare for reading
  $db, $pj = from_db, from_proj

  # Construct a hash to store columns and cells we are transferring
  print_debug("Fetching columns...")
  begin
    col_map = Hash.new
    cell_map = Hash.new
    for col in varnames
      c = getColumn(col.to_s)
      if c.nil?
        puts "Warning: column #{c} not found! Skipping..."
        next
      end
      col_map[col] = c
      cell_map[col] = c.cells
      print_debug("Read column : #{col.to_s}")
    end
  end

  # Set working database to destination database to prepare for writing
  $db, $pj = to_db, to_proj

  # Go through the hashmaps and reconstruct the columns
  begin
    for key in col_map.keys
      col = col_map[key]
      cells = cell_map[key]
      arglist = col.arglist

      # Construct a new variable and add all associated cells
      newvar = createVariable(key.to_s, arglist)
      for cell in cells
        c = newvar.make_new_cell()
        # Clone the existing cell arguments to the new cell.
        cell.arglist.each { |x|
          c.change_arg(x, cell.get_arg(x))
        }
        c.ordinal = cell.ordinal
        c.onset = cell.onset
        c.offset = cell.offset
      end
      setVariable(key.to_s, newvar)
      print_debug("Wrote column : #{key.to_s} with #{newvar.cells.length} cells")
    end
  rescue StandardError => e
    puts "Failed trying to write column #{col}"
    puts e.message
    puts e.backtrace
    return nil
  end

  # Save the database to file if applicable
  saveDB(db2path) if db2path!=""

  # Final step: take care of deleting columns from source database if option is set.
  if remove
    $db, $pj = from_db, from_proj

    # Use our hashmap since it takes care of improper column names (returned nil from getColumn())
    col_map.keys.each { |x|
      delete_column(x.to_s)
    }

    saveDB(db1path) if db1path!=""
  end

  # Restore the saved database and project globals
  $db, $pj = saved_db, saved_proj

  puts "Transfer completed successfully!"
end
alias :transfer_column :transfer_columns
alias :transferColumns :transfer_columns
alias :transferColumn :transfer_columns
alias :transferVariables :transfer_columns
alias :transferVariable :transfer_columns

# Do a quick, in Datavyu, check of reliability errors.
# @param main_col [String, RColumn] Either the string name or the Ruby column from get_column of the primary column to compare against.
# @param rel_col [String, RColumn] Either the string name or the Ruby column from get_column of the reliability column to compare to the primary column.
# @param match_arg [String] The string of the argument to use to match the relability cells to the primary cells.  This must be a unique identifier between the cells.
# @param time_tolerance [Integer] The amount of slack you allow, in milliseconds, for difference between onset and offset before it is considered an error.  Set to 0
#     for no difference allowed and to a very large number for infinite distance allowed.
# @param dump_file [String, File] (optional): The full string path to dump the relability output to.  This
#     can be used for multi-file dumps or just to keep a log.  You can also give it a Ruby
#     File object if a file is already started.
# @return [nil]
# @example
#   check_reliability("trial", "rel.trial", "trialnum", 100, "/Users/motoruser/Desktop/Relcheck.txt")
#   check_reliability("trial", "rel.trial", "trialnum", 100)
def check_reliability(main_col, rel_col, match_arg, time_tolerance, *dump_file)
  # Make the match_arg conform to the method format that is used
  if ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9"].include?(match_arg[0].chr)
    match_arg = match_arg[1..match_arg.length]
  end
  match_arg = match_arg.gsub(/(\W)+/, "").downcase

  # Set up our method variables
  dump_file = dump_file[0]
  if main_col.class == "".class
    main_col = get_column(main_col)
  end
  if rel_col.class == "".class
    rel_col = get_column(rel_col)
  end

  printing = false
  if dump_file != nil
    if dump_file.class == "".class
      dump_file = open(dump_file, 'a')
    end
    printing = true
  end

  # Define interal function for printing errors
  def print_err(m_cell, r_cell, arg, dump_file, main_col, rel_col)
    main_val = eval "m_cell.#{arg}"
    rel_val = eval "r_cell.#{arg}"
    err_str = "ERROR in " + main_col.name + " at Ordinal " + m_cell.ordinal.to_s + ", rel ordinal " + r_cell.ordinal.to_s + " in argument " + arg + ": " + main_val.to_s + ", " + rel_val.to_s + "\n"
    if dump_file != nil
      dump_file.write(err_str)
    end
    print err_str
  end

  # Build error array
  errors = Hash.new
  for arg in main_col.arglist
    errors[arg] = 0
  end
  errors["onset"] = 0
  errors["offset"] = 0

  # Now check the cells
  for mc in main_col.cells
    main_bind = eval "mc.#{match_arg}"
    for rc in rel_col.cells
      rel_bind = eval "rc.#{match_arg}"
      if main_bind == rel_bind
        # Then check these cells match, check them for errors
        if (mc.onset - rc.onset).abs >= time_tolerance
          print_err(mc, rc, "onset", dump_file, main_col, rel_col)
          errors["onset"] = errors["onset"] + 1
        end
        if (mc.offset - rc.offset).abs >= time_tolerance
          print_err(mc, rc, "offset", dump_file, main_col, rel_col)
          errors["offset"] = errors["offset"] + 1
        end

        for arg in main_col.arglist
          main_val = eval "mc.#{arg}"
          rel_val = eval "rc.#{arg}"
          if main_val != rel_val
            print_err(mc, rc, arg, dump_file, main_col, rel_col)
            errors[arg] = errors[arg] + 1
          end
        end
      end
    end
  end

  for arg, errs in errors
    str = "Total errors for " + arg + ": " + errs.to_s + ", Agreement:" + "%.2f" % (100 * (1.0 - (errs / rel_col.cells.length.to_f))) + "%\n"
    print str
    if dump_file != nil
      dump_file.write(str)
      dump_file.flush()
    end
  end

  return errors, rel_col.cells.length.to_f
end
alias :checkReliability :check_reliability
alias :check_rel :check_reliability
alias :checkRel :check_reliability


# Check reliability for a continuous coding pass. Merge the two coders' columns and
# filter out the agreements. Returns a column with cells for regions where coders disagreed.
# @param pri_col [String, RColumn] Primary's coder's column
# @param rel_col [String, RColumn] Reliability coder's column
# @param block_col [String, RColumn] Column containing coding block. Use nil if none.
# @param time_threshold [Integer] The amount of slack you allow, in milliseconds, for difference between onset and offset before it is considered a disagreement.
# @param codes_to_check [Array<String>] List of codes to compare between the coders
# @return [RColumn] column with cells for disagreements
# @example
#   check_reliability_continuous('reach', 'reach_rel', 'reach_blocks', 100, 'hand', 'touch')
# @since 1.4.0
def check_reliability_continuous(pri_col, rel_col, block_col, time_threshold, *codes_to_check)
  p_col = (pri_col.class == String)? get_column(pri_col) : pri_col
  r_col = (rel_col.class == String)? get_column(rel_col) : rel_col
  unless (block_col.nil? || block_col == '')
    b_col = (block_col.class == String)? get_column(block_col) : block_col
  end

  raise "Invalid columns" unless p_col.class == RColumn && r_col.class == RColumn

  # Build a set to store tuples of (onset,offset).  We need this to find cells with duration
  # less than mutex_threshold that have been entirely missed by one of the coders.
  interval_map = (p_col.cells + r_col.cells).map{ |x| [x.onset.to_i,x.offset.to_i] }.uniq

  m_col = merge_columns('disagreements', pri_col, rel_col)

  # Remove cells not contained by block column
  unless b_col.nil?
    m_col.cells.reject! do |slice|
      !(b_col.cells.any?{ |x| x.contains(slice) } )
    end
  end

  p_prefix = p_col.name.downcase
  r_prefix = r_col.name.downcase
  # Iterate over merged column cells and filter out agreements.
  m_col.cells.reject! do |slice|
    dur = slice.duration
    num_coders = [slice.get_code("#{p_prefix}_ordinal"), slice.get_code("#{r_prefix}_ordinal")].reject{ |x| x == '' }.size

    whole_cell = interval_map.include?([slice.onset, slice.offset])

    pri_codes = codes_to_check.map{ |x| slice.get_code("#{p_prefix}_#{x}") }
    rel_codes = codes_to_check.map{ |x| slice.get_code("#{r_prefix}_#{x}") }
    code_differs = pri_codes.zip(rel_codes).any?{ |pair| pair.first != pair.last }

    flag_duration = (num_coders == 1) && (dur >= time_threshold)
    flag_missed = (num_coders == 1) && whole_cell
    flag_code = (num_coders == 2) && code_differs
    # p [flag_duration, flag_missed, flag_code]
    !(flag_duration || flag_missed || flag_code)
  end

  return m_col
end

# Do a quick, in Datavyu, check of valid codes.
# @param var [String, RColumn] name of column to check
# @param dump_file [String, File] output file to print messages to. Use '' to print to console.
# @param arg_code_pairs [Array<String>]  A list of the argument names and valid codes
#     in the following format: "argument_name", ["y","n"], "argument2", ["j","k","m"]
# @example
#  check_valid_codes("trial", "", "hand", ["l","r","b","n"], "turn", ["l","r"], "unit", [1,2,3])
def check_valid_codes(var, dump_file, *arg_code_pairs)
  if var.class == "".class
    var = get_column(var)
  end

  if dump_file != ""
    if dump_file.class == "".class
      dump_file = open(dump_file, 'a')
    end
  end

  # Make the argument/code hash
  arg_code = Hash.new
  for i in 0...arg_code_pairs.length
    if i % 2 == 0
      if arg_code_pairs[i].class != "".class
        print_debug 'FATAL ERROR in argument/valid code array.  Exiting.  Please check to make sure it is in the format "argumentname", ["valid","codes"]'
        exit
      end
      arg = arg_code_pairs[i]
      if ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9"].include?(arg[1].chr)
        arg = arg[1..arg.length]
      end
      arg = arg.gsub(/(\W )+/, "").downcase

      arg_code[arg] = arg_code_pairs[i+1]
    end
  end

  errors = false
  for cell in var.cells
    for arg, code in arg_code
      val = eval "cell.#{arg}"
      if not code.include?(val)
        errors = true
        str = "Code ERROR: Var: " + var.name + "\tOrdinal: " + cell.ordinal.to_s + "\tArg: " + arg + "\tVal: " + val + "\n"
        print str
        if dump_file != ""
          dump_file.write(str)
        end
      end
    end
  end
  if not errors
    print_debug "No errors found."
  end
end
alias :checkValidCodes :check_valid_codes

# Check valid codes on cells in a column using regex. Backwards-compatible with checkValidCodes
# @since 1.3.6
# @param data [String, RColumn, Hash] When this parameter is a String or a column object from get_column(), the function operates on codes within this column. If the parameter is a Hash (associative array), the function ignores the arg_code_pairs arguments and uses data from this Hash. The Hash must be structured as a nested mapping from columns (either as Strings or RColumns) to Hashes. These nested hashes must be mappings from code names (as Strings) to valid code values (as either lists (Arrays) or patterns (Regexp)).
# @param outfile [String, File] The full path of the file to print output to. Use '' to print to scripting console.
# @param arg_filt_pairs Pairs of code name and acceptable values either as an array of values or regexp. Ignored if first parameter is a Hash.
# @return nothing
#
# @example
#   checkValidCodes2("trial", "", "hand", ["l","r","b","n"], "turn", ["l","r"], "unit", /\A\d+\Z/)
def check_valid_codes2(data, outfile, *arg_filt_pairs)
	if data.class == "".class
		data = get_column(data)
  elsif data.class == Hash
    # data is already a hashmap
    map = data
	end

	unless outfile == ""
		if outfile.class == "".class
	    	outfile = open(File.expand_path(outfile), 'a')
	  	end
	end

  # Create a map if a mapping wasn't passed in. Mostly for backwards compatibility with checkValidCodes().
  if map.nil?
    map = Hash.new

  	# Make the argument/code hash
  	arg_code = Hash.new
  	for i in 0...arg_filt_pairs.length
  	  if i % 2 == 0
    		if arg_filt_pairs[i].class != "".class
    			print_debug 'FATAL ERROR in argument/valid code array.  Exiting.  Please check to make sure it is in the format "argumentname", ["valid","codes"]'
    			exit
    		end

    		arg = arg_filt_pairs[i]
    		if ["0","1","2","3","4","5","6","7","8","9"].include?(arg[1].chr)
    			arg = arg[1..arg.length]
    		end
    		arg = arg.gsub(/(\W )+/,"").downcase

  	    # Add the filter for this code.  If the given filter is an array, convert it to a regular expression using Regex.union
        arg_code[arg] = arg_filt_pairs[i+1]
  	  end
  	end

    map[data] = arg_code
  end

	errors = false
  # Iterate over key,entry (column, valid code mapping) in map
  map.each_pair do |var, col_map|
    var = get_column(var) if var.class == String

    # Iterate over cells in var and check each code's value
  	for cell in var.cells
      for arg, filt in col_map
      	val = eval "cell.#{arg}"
        # Check whether value is valid — different functions depending on filter type
        valid = case # note: we can't use case on filt.class because case uses === for comparison
        when filt.class == Regexp
        	!(filt.match(val).nil?)
        when filt.class == Array
          filt.include?(val)
        else
          raise "Unhandled filter type: #{filt.class}"
        end

        if !valid
          errors = true
          str = "Code ERROR: Var: " + var.name + "\tOrdinal: " + cell.ordinal.to_s + "\tArg: " + arg + "\tVal: " + val + "\n"
          print str
          outfile.write(str) unless outfile == ""
        end
      end
  	end
  end
	unless errors
  	print_debug "No errors found."
	end
end
alias :checkValidCodes2 :check_valid_codes2

# Check valid codes on cells in a column using regex. Not backwards-compatible with check_valid_codes().
# @since 1.3.6
# @param map [Hash] The Hash must be structured as a nested mapping from columns (either as Strings or RColumns) to Hashes. These nested hashes must be mappings from code names (as Strings) to valid code values (as either lists (Arrays) or patterns (Regexp)).
# @param outfile [String, File] The full path of the file to print output to. Omit to print only to console.
# @return number of detected errors
# @return a list containing all error messages
def check_valid_codes3(map, outfile = nil)
  # Open outfile if given
  unless outfile.nil?
    outfile = open(File.expand_path(outfile), 'a') if outfile.class == ''.class
  end

  errors = []
  err_count = 0
  # Iterate over key,entry (column, valid code mapping) in map
  map.each_pair do |var, col_map|
    var = get_column(var) if var.class == String

    # Iterate over cells in var and check each code's value
  	var.cells.each do |cell|
      col_map.each_pair do |code, filt|
      	val = cell.get_code(code)
        # Check whether value is valid — different functions depending on filter type
        valid = case # note: we can't use case on filt.class because case uses === for comparison
        when filt.class == Regexp
        	!(filt.match(val).nil?)
        when filt.class == Array
          filt.include?(val)
        when filt.class == Proc
          filt.call(val)
        else
          raise "Unhandled filter type: #{filt.class}"
        end

        if !valid
          err_count += 1
          row = [var.name, cell.ordinal, code, val].join("\t")
          errors << row
        end
      end
  	end
  end

  if(err_count > 0)
    print_debug "Found #{errors} errors."
    header = (%w(COLUMN CELL_ORDINAL CODE VALUE)).join("\t")
    puts header
    puts errors
    unless outfile.nil?
      outfile.puts header
      outfile.puts errors
      outfile.close
    end
	end

  return [err_count, errors]
end

# Return a list of columns from the current spreadsheet.
# @return [Array]
def get_column_list()
  name_list = Array.new
  vars = $db.getAllVariables()
  for v in vars
    name_list << v.name
  end

  return name_list
end
alias :getColumnList :get_column_list
alias :getVariableList :get_column_list

# TODO: Finish?
#++ Incomplete method.
def print_all_nested(file)
  columns = getColumnList()
  columns.sort! # This is just so everything is the same across runs, regardless of column order
  # Scan each column, getting a list of how many cells the cells of that
  # contain and how much time the cells of that column fill

  times = Hash.new

  for outer_col in columns
    collected_time = 0
    for cell in outer_col.cells
      collected_time += cell.offset - cell.onset
    end
    times[outer_col.name] = collected_time
  end

  # Now, we want to loop over the columns in the order of the amount of data
  # that they take up.

end
alias :printAllNested :print_all_nested
private :print_all_nested

# Makes temporally adjacent cells in a column continuous if the interval between them is below a given threshold.
# @param colname [String] name of column to smooth
# @param tol [Integer] milliseconds below which cell onset should be changed to make continuous
# @return nil
# @note Only the onset is changed; not the offset.
def smooth_column(colname, tol=33)
  col = get_column(colname)
  for i in 0..col.cells.length-2
    curcell = col.cells[i]
    nextcell = col.cells[i+1]

    if nextcell.onset - curcell.offset < tol
      nextcell.change_arg("onset", curcell.offset)
    end
  end
  setVariable(colname, col)
end
alias :smoothColumn :smooth_column

# Outputs the values of all codes specified from the given cell to the given output file.
# Row is delimited by tabs.
# @param cell [RCell] cell to print codes from
# @param file [File] output file
# @return nil
def print_codes(cell, file, args)
  for a in args
    #puts "Printing: " + a
    val = eval "cell.#{a}"
    file.write(val.to_s + "\t")
  end
end
alias :print_args :print_codes

# Finds the first cell in the specified column that overlaps the given time.
# @param col [RColumn] column to find cell from
# @param time [Integer] time in milliseconds
# @return [RCell] Cell that spans the given time; nil if none found.
def get_cell_from_time(col, time)
  for cell in col.cells
    if cell.onset <= time and cell.offset >= time
      return cell
    end
  end
  return nil
end
alias :getCellFromTime :get_cell_from_time

# Returns ordinal, onset, offset, and the values of all codes from the given cell.
# TODO change method name to something more appropriate
# @param cell [RCell] cell whose codes to print
# @return [Array<String>] array of values for all codes in cell
def print_cell_codes(cell)
  s = Array.new
  s << cell.ordinal.to_s
  s << cell.onset.to_s
  s << cell.offset.to_s
  for arg in cell.arglist
    s << cell.get_arg(arg)
  end
  return s
end
alias :printCellCodes :print_cell_codes
alias :printCellArgs :print_cell_codes

# Delete a cell from the spreadsheet
# @param cell [RCell] Cell to delete
# @return [nil]
def delete_cell(cell)
  cell.db_cell.getVariable.removeCell(cell.db_cell)
end
alias :deleteCell :delete_cell

# Return the OS version
# @return [String] 'windows', 'mac', or 'linux'
# @example
#  filepath = (getOS() == 'windows')? 'C:\data' : '~/data'
def get_os
	host_os = RbConfig::CONFIG['host_os']
	case host_os
	when /mswin|msys|mingw|cygwin|bccwin|wince|emc/
		os = 'windows'
	when /darwin|mac os/
		os = 'mac'
	when /linux|solaris|bsd/
		os = 'linux'
	else
		raise "Unknown OS: #{host_os.inspect}"
	end
	return os
end
alias :getOS :get_os

# Return Datavyu version string.
# @return [String] Version string in the fromat "v.:#.#"
def get_datavyu_version
  return org.datavyu.util.DatavyuVersion.getLocalVersion.getVersion
end
alias :getDatavyuVersion :get_datavyu_version

# Move a column to the desired destination.
# Pass in list, show only those columns, in that order
# List can be of vars or strings
# @param column_list [Array(String)] The list of columns that we want to show, in the order we want them shown.
def set_column_order(*column_list)
  column_list.flatten!
  return if column_list.empty?

  # Move the given columns to positions 0..n using shuffle_column()
  column_list.reverse.each_with_index do |col, i|
    vars = $sp.get_spreadsheet_panel().get_columns()
    for j in 0...vars.size()
      if vars[j].get_column_name() == col
        $sp.get_spreadsheet_panel().shuffle_column(j, i)
      end
    end
  end

  # Now get all variables, and if they are not in column list, hide them
  vars = $sp.get_spreadsheet_panel().get_columns()
  for v in vars
    if column_list.include?(v.get_column_name())
      show_columns(v.get_column_name())
    else
      hide_columns(v.get_column_name())
    end
  end
end

# Check whether current Datavyu version falls within the specified minimum and maximum versions (inclusive)
# @param minVersion [String] Minimum version (e.g. 'v:1.3.6')
# @param maxVersion [String] Maximum version. If unspecified, no upper bound is checked.
# @return [true, false] true if min,max version check passes; false otherwise.
def check_datavyu_version(minVersion, maxVersion = nil)
  currentVersion = get_datavyu_version()
  minCheck = (minVersion <=> currentVersion) <= 0
  maxCheck = (maxVersion.nil?)? true : (currentVersion <=> maxVersion) <= 0

  return minCheck && maxCheck
end
alias :checkDatavyuVersion :check_datavyu_version

# Return list of *.opf files from given directory.
# @param dir [String] directory to check
# @param recurse [true, false] true to check subfolders, false to only check given folder
# @return [Array<String>] list containing names of .opf files.
# @note When recurse is set true, names of Datavyu files in nested folders will be the relative path from the starting directory; e.g. 'folder1/folder2/my_datavyu_file.opf'
# @example
#   input_files = get_datavyu_files_from('~/Desktop/input', true)
def get_datavyu_files_from(dir, recurse=false)
  dir = File.expand_path(dir)
  pat = recurse ? '**/*.opf' : '*.opf'
  files = Dir.chdir(dir){ Dir.glob(pat) }
  return files
end

# Hide the given columns in the spreadsheet
# @param names [Array<String>] names of columns to hide
def hide_columns(*names)
  names.flatten!
  valid_names = names & get_column_list
  valid_names.each{ |x| $db.getVariable(x).setHidden(true)}
end

# Show the given columns in the spreadsheet
# @param names [Array<String>] names of columns to show
def show_columns(*names)
  names.flatten!
  valid_names = names & get_column_list
  valid_names.each{ |x| $db.getVariable(x).setHidden(false) }
end

# Add a video to the current spreadsheet/controller.
# @param filepath [String] path to the file
# @param plugin [String] short name of plugin to load video with; currently supprots jfx, nativeosx, ffmpeg
# @param onset [Integer] start point of video in milliseconds
# @param timeout [Integer] seconds to wait for the video to load up
# @return [True, False] true if filepath and plugin are valid, false otherwise
# @since 1.4.2
def new_video(filepath, plugin, onset=0, timeout=5)
  id = Datavyu.getVideoController.openVideo(filepath, plugin)
  return false if id.nil?

  success = false
  # Attempt to set track offset until timeout
  tracks = Datavyu.getVideoController.getMixerController.getTracksEditorController
  t = Time.now
  while (Time.now - t).to_f < timeout
    success = tracks.setTrackOffset(id, onset)
    break if success
    sleep(0.5)
  end

  puts "WARNING: added video but timed out trying to set start point." unless success
  
  return success
end
